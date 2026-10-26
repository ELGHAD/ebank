package com.example.ebank.billing;

import com.example.ebank.IntegrationTestSupport;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.OnboardClientRequest;
import com.example.ebank.entities.AppRole;
import com.example.ebank.entities.Organization;
import com.example.ebank.exception.QuotaExceededException;
import com.example.ebank.services.ClientService;
import com.example.ebank.transfer.TransferDtos.TransferRequest;
import com.example.ebank.transfer.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Plans, quotas (also under concurrency), plan changes, simulated payment states and renewal. */
class BillingIntegrationTest extends IntegrationTestSupport {

    @Autowired BillingService billingService;
    @Autowired ClientService clientService;
    @Autowired TransferService transferService;

    Organization org;
    String adminToken;
    String agentToken;

    @BeforeEach
    void setUp() throws Exception {
        org = newOrganization();
        adminToken = staffToken(org, AppRole.ORG_ADMIN);
        agentToken = agentToken(org);
    }

    private ResultActions changePlan(String planCode) throws Exception {
        return mvc.perform(put("/api/org/subscription/plan")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planCode\": \"" + planCode + "\"}"));
    }

    private ResultActions setSubscriptionStatus(String status) throws Exception {
        return mvc.perform(patch("/api/platform/organizations/" + org.getId() + "/subscription-status")
                .header("Authorization", bearer(staffToken(null, AppRole.PLATFORM_ADMIN)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"" + status + "\"}"));
    }

    private AppUserPrincipal agentPrincipal() {
        var agent = userRepo.findAll().stream()
                .filter(u -> u.getOrganization() != null && u.getOrganization().getId().equals(org.getId())
                        && u.getRoles().stream().anyMatch(r -> r.getRoleName().equals(AppRole.AGENT)))
                .findFirst().orElseThrow();
        return new AppUserPrincipal(agent.getId(), agent.getUsername(), org.getId());
    }

    private void setTransfersThisMonth(int count) {
        jdbc.update("""
                insert into usage_counter (organization_id, period_month, transfers) values (?, ?, ?)
                on conflict (organization_id, period_month) do update set transfers = excluded.transfers""",
                org.getId(), LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1), count);
    }

    private int transfersThisMonth() {
        return jdbc.queryForObject("select transfers from usage_counter where organization_id = ? and period_month = ?",
                Integer.class, org.getId(), LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1));
    }

    @Test
    void everyNewOrganizationStartsOnFreeThroughTheDatabaseTrigger() throws Exception {
        onboardClient(agentToken);

        getWithToken("/api/org/subscription", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.code").value("FREE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.usage[?(@.quota == 'CLIENTS')].used", contains(1)))
                .andExpect(jsonPath("$.usage[?(@.quota == 'CLIENTS')].limit", contains(25)))
                .andExpect(jsonPath("$.usage[?(@.quota == 'STAFF')].used", contains(2)))
                .andExpect(jsonPath("$.invoices", hasSize(0)));
        getWithToken("/api/org/plans", adminToken)
                .andExpect(jsonPath("$[*].code", contains("FREE", "PRO", "ENTERPRISE")))
                .andExpect(jsonPath("$[2].maxClients").doesNotExist());
    }

    @Test
    void clientQuotaIsEnforcedWithAClearMessage() throws Exception {
        for (int i = 0; i < 25; i++) {
            onboardClient(agentToken);
        }
        mvc.perform(post("/api/admin/clients")
                        .header("Authorization", bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identityRef": "%s", "firstName": "One", "lastName": "Toomany", "email": "%s@test.ebank"}"""
                                .formatted(unique("ID"), unique("over"))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("QUOTA_EXCEEDED"))
                .andExpect(jsonPath("$.quota").value("CLIENTS"))
                .andExpect(jsonPath("$.limit").value(25))
                .andExpect(jsonPath("$.detail", containsString("Free plan allows 25 clients")));
        assertThat(jdbc.queryForObject("select count(*) from client where organization_id = ?", Long.class, org.getId()))
                .isEqualTo(25);
    }

    @Test
    void staffQuotaAlsoAppliesToReEnablingSomeone() throws Exception {
        // Free allows 3 staff: the admin and the agent from setUp, plus one more
        mvc.perform(post("/api/org/staff").header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + unique("third") + "@test.ebank\", \"role\": \"AGENT\"}")).andExpect(status().isCreated());
        mvc.perform(post("/api/org/staff").header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + unique("fourth") + "@test.ebank\", \"role\": \"AGENT\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.quota").value("STAFF"));

        long agentId = agentPrincipal().userId();
        mvc.perform(patch("/api/org/staff/" + agentId + "/enabled").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\": false}")).andExpect(status().isOk());
        mvc.perform(post("/api/org/staff").header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + unique("replacement") + "@test.ebank\", \"role\": \"AGENT\"}")).andExpect(status().isCreated());
        mvc.perform(patch("/api/org/staff/" + agentId + "/enabled").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\": true}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.quota").value("STAFF"));
    }

    @Test
    void monthlyTransferQuotaIsEnforcedAndOnlySuccessfulTransfersCount() throws Exception {
        OnboardedClient alice = onboardClient(agentToken);
        AccountInfo from = newAccount(agentToken, alice.id(), "100");
        AccountInfo to = newAccount(agentToken, onboardClient(agentToken).id(), "0");
        var caller = new AppUserPrincipal(userRepo.findByUsername(alice.username()).orElseThrow().getId(), alice.username(), org.getId());
        setTransfersThisMonth(198);

        // A transfer that fails after being counted is rolled back, counter included
        assertThatThrownBy(() -> transferService.transfer(caller, UUID.randomUUID().toString(),
                new TransferRequest(from.id(), to.rib(), BigDecimal.ONE, "x".repeat(141))));
        assertThat(transfersThisMonth()).isEqualTo(198);

        transferService.transfer(caller, UUID.randomUUID().toString(), new TransferRequest(from.id(), to.rib(), BigDecimal.ONE, null));
        transferService.transfer(caller, UUID.randomUUID().toString(), new TransferRequest(from.id(), to.rib(), BigDecimal.ONE, null));
        assertThatThrownBy(() -> transferService.transfer(caller, UUID.randomUUID().toString(),
                new TransferRequest(from.id(), to.rib(), BigDecimal.ONE, null)))
                .isInstanceOfSatisfying(QuotaExceededException.class, e -> assertThat(e.getQuota()).isEqualTo("TRANSFERS_PER_MONTH"));

        assertThat(transfersThisMonth()).isEqualTo(200);
        assertThat(balanceOf(from.id())).isEqualByComparingTo("98.00");
        assertBooksBalanced();
    }

    private static List<Object> runConcurrently(List<Supplier<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Supplier<Object> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        return task.get();
                    } catch (RuntimeException e) {
                        return e;
                    }
                }));
            }
            start.countDown();
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> f : futures) {
                outcomes.add(f.get(60, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentTransfersCannotExceedTheMonthlyQuota() throws Exception {
        OnboardedClient alice = onboardClient(agentToken);
        AccountInfo from = newAccount(agentToken, alice.id(), "1000");
        AccountInfo to = newAccount(agentToken, onboardClient(agentToken).id(), "0");
        var caller = new AppUserPrincipal(userRepo.findByUsername(alice.username()).orElseThrow().getId(), alice.username(), org.getId());
        setTransfersThisMonth(195);

        List<Supplier<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            tasks.add(() -> transferService.transfer(caller, UUID.randomUUID().toString(),
                    new TransferRequest(from.id(), to.rib(), BigDecimal.ONE, null)));
        }
        List<Object> outcomes = runConcurrently(tasks);

        assertThat(outcomes).filteredOn(o -> o instanceof TransferService.TransferResult).hasSize(5);
        assertThat(outcomes).filteredOn(o -> o instanceof QuotaExceededException).hasSize(7);
        assertThat(transfersThisMonth()).isEqualTo(200);
        assertThat(balanceOf(to.id())).isEqualByComparingTo("5.00");
    }

    @Test
    void concurrentOnboardingCannotExceedTheClientQuota() throws Exception {
        for (int i = 0; i < 23; i++) {
            onboardClient(agentToken);
        }
        AppUserPrincipal agent = agentPrincipal();
        List<Supplier<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tasks.add(() -> clientService.onboardClient(agent, new OnboardClientRequest(
                    unique("ID"), "Race", "Client", null, unique("race") + "@test.ebank", null)));
        }
        List<Object> outcomes = runConcurrently(tasks);

        assertThat(outcomes).filteredOn(o -> o instanceof QuotaExceededException).hasSize(4);
        assertThat(jdbc.queryForObject("select count(*) from client where organization_id = ?", Long.class, org.getId()))
                .isEqualTo(25);
    }

    @Test
    void upgradingIssuesASimulatedInvoiceAndDowngradingChecksUsage() throws Exception {
        changePlan("PRO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.code").value("PRO"))
                .andExpect(jsonPath("$.invoices", hasSize(1)))
                .andExpect(jsonPath("$.invoices[0].amount").value(49.0))
                .andExpect(jsonPath("$.invoices[0].currency").value("EUR"));

        // 4 staff now (admin, agent + 2): above Free's 3, so the downgrade is refused with the reason
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/org/staff").header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\": \"" + unique("pro") + "@test.ebank\", \"role\": \"AGENT\"}")).andExpect(status().isCreated());
        }
        changePlan("FREE")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_LIMITS_EXCEEDED"))
                .andExpect(jsonPath("$.detail", containsString("4 staff members (limit 3)")));

        changePlan("ENTERPRISE").andExpect(jsonPath("$.plan.code").value("ENTERPRISE"))
                .andExpect(jsonPath("$.invoices", hasSize(2)))
                .andExpect(jsonPath("$.usage[?(@.quota == 'STAFF')].limit", contains(nullValue())));
        changePlan("UNKNOWN").andExpect(status().isNotFound());

        getWithToken("/api/admin/audit-events?type=SUBSCRIPTION_PLAN_CHANGED", adminToken)
                .andExpect(jsonPath("$.totalElements").value(2));
        getWithToken("/api/admin/audit-events?type=INVOICE_ISSUED", adminToken)
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void pastDueBlocksGrowthButNeverCustomersMoney() throws Exception {
        OnboardedClient alice = onboardClient(agentToken);
        AccountInfo from = newAccount(agentToken, alice.id(), "50");
        AccountInfo to = newAccount(agentToken, onboardClient(agentToken).id(), "0");
        String aliceToken = clientToken(alice);

        setSubscriptionStatus("PAST_DUE").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAST_DUE"));

        mvc.perform(post("/api/admin/clients").header("Authorization", bearer(agentToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identityRef": "%s", "firstName": "A", "lastName": "B", "email": "%s@test.ebank"}"""
                                .formatted(unique("ID"), unique("blocked"))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_PAST_DUE"));
        openAccount(agentToken, alice.id(), "").andExpect(jsonPath("$.code").value("SUBSCRIPTION_PAST_DUE"));
        changePlan("PRO").andExpect(jsonPath("$.code").value("SUBSCRIPTION_PAST_DUE"));

        // Customers keep full access to their money
        mvc.perform(post("/api/client/transfers").header("Authorization", bearer(aliceToken))
                        .header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\": " + from.id() + ", \"destinationRib\": \"" + to.rib() + "\", \"amount\": 10}"))
                .andExpect(status().isCreated());
        getWithToken("/api/client/accounts/" + from.id() + "/entries", aliceToken).andExpect(status().isOk());

        setSubscriptionStatus("ACTIVE").andExpect(jsonPath("$.status").value("ACTIVE"));
        onboardClient(agentToken);
    }

    @Test
    void renewalIssuesOneInvoicePerMissedPeriodAndIsIdempotent() throws Exception {
        changePlan("PRO").andExpect(status().isOk());
        // Pretend the subscription started three and a half months ago
        jdbc.update("""
                update subscription set current_period_start = now() - interval '3 months 15 days',
                                        current_period_end = now() - interval '2 months 15 days'
                where organization_id = ?""", org.getId());

        billingService.renewDueSubscriptions();
        billingService.renewDueSubscriptions(); // a second run (or a second instance) changes nothing

        getWithToken("/api/org/subscription", adminToken)
                .andExpect(jsonPath("$.invoices", hasSize(4))) // the upgrade + 3 renewals
                .andExpect(jsonPath("$.invoices[*].amount", everyItem(is(49.0))));
        assertThat(jdbc.queryForObject("select current_period_end > now() from subscription where organization_id = ?",
                Boolean.class, org.getId())).isTrue();
    }

    @Test
    void freeAndPastDueRenewalsAdvanceThePeriodWithoutInvoices() {
        jdbc.update("update subscription set current_period_start = now() - interval '40 days', "
                + "current_period_end = now() - interval '10 days' where organization_id = ?", org.getId());
        billingService.renewDueSubscriptions();
        assertThat(jdbc.queryForObject("select count(*) from invoice where organization_id = ?", Long.class, org.getId()))
                .isZero();
        assertThat(jdbc.queryForObject("select current_period_end > now() from subscription where organization_id = ?",
                Boolean.class, org.getId())).isTrue();
    }

    @Test
    void onlyTheRightRolesReachBillingEndpoints() throws Exception {
        getWithToken("/api/org/subscription", agentToken).andExpect(status().isForbidden());
        mvc.perform(patch("/api/platform/organizations/" + org.getId() + "/subscription-status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isForbidden());
        getWithToken("/api/platform/organizations?size=100", staffToken(null, AppRole.PLATFORM_ADMIN))
                .andExpect(jsonPath("$.content[?(@.id == " + org.getId() + ")].plan", contains("FREE")))
                .andExpect(jsonPath("$.content[?(@.id == " + org.getId() + ")].subscriptionStatus", contains("ACTIVE")));
    }

    // Least privilege: the application cannot change plan prices or rewrite invoices
    @Test
    void applicationCannotEditPlansOrInvoices() throws Exception {
        changePlan("PRO").andExpect(status().isOk());
        assertThatThrownBy(() -> jdbc.update("update plan set monthly_price = 0 where code = 'PRO'"))
                .rootCause().hasMessageContaining("permission denied");
        assertThatThrownBy(() -> jdbc.update("delete from invoice where organization_id = ?", org.getId()))
                .rootCause().hasMessageContaining("permission denied");
        assertThatThrownBy(() -> ownerJdbc().update("update invoice set amount = 0 where organization_id = ?", org.getId()))
                .rootCause().hasMessageContaining("append-only");
    }
}
