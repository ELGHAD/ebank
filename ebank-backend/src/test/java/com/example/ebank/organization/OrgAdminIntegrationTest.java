package com.example.ebank.organization;

import com.example.ebank.IntegrationTestSupport;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.config.AuthCookies;
import com.example.ebank.exception.BusinessRuleException;
import com.example.ebank.entities.AppRole;
import com.example.ebank.entities.Organization;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Organization administration: overview and staff management. */
class OrgAdminIntegrationTest extends IntegrationTestSupport {

    @Autowired OrgAdminService orgAdminService;

    Organization org;
    String admin;
    String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        org = newOrganization();
        admin = newStaff(org, AppRole.ORG_ADMIN);
        adminToken = token(admin, AGENT_PASSWORD);
    }

    private ResultActions createStaff(String token, String email, String role) throws Exception {
        return mvc.perform(post("/api/org/staff")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "role": "%s"}""".formatted(email, role)));
    }

    private ResultActions setEnabled(String token, long userId, boolean enabled) throws Exception {
        return mvc.perform(patch("/api/org/staff/" + userId + "/enabled")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\": " + enabled + "}"));
    }

    private long idOf(String username) {
        return userRepo.findByUsername(username).orElseThrow().getId();
    }

    @Test
    void overviewSummarizesTheOrganization() throws Exception {
        String agentToken = agentToken(org);
        OnboardedClient alice = onboardClient(agentToken);
        AccountInfo a1 = newAccount(agentToken, alice.id(), "100.50");
        newAccount(agentToken, alice.id(), "20", "EUR");
        AccountInfo bob = newAccount(agentToken, onboardClient(agentToken).id(), "0");
        mvc.perform(post("/api/client/transfers")
                        .header("Authorization", bearer(clientToken(alice)))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountId": %d, "destinationRib": "%s", "amount": 30}""".formatted(a1.id(), bob.rib())))
                .andExpect(status().isCreated());
        onboardClient(agentToken(newOrganization())); // other organization: not counted

        getWithToken("/api/org/overview", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value(org.getName()))
                .andExpect(jsonPath("$.clients").value(2))
                .andExpect(jsonPath("$.accounts").value(3))
                .andExpect(jsonPath("$.activeAccounts").value(3))
                .andExpect(jsonPath("$.balances[?(@.currency == 'MAD')].amount", contains(100.50)))
                .andExpect(jsonPath("$.balances[?(@.currency == 'EUR')].amount", contains(20.0)))
                .andExpect(jsonPath("$.transfersLast30Days").value(1))
                .andExpect(jsonPath("$.transferVolumeLast30Days[0].amount").value(30.0))
                .andExpect(jsonPath("$.staff").value(2));
    }

    @Test
    void createdAgentMustChangePasswordThenCanWork() throws Exception {
        String email = unique("new-agent") + "@test.ebank";
        String body = createStaff(adminToken, email, "AGENT")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.member.roles", contains("AGENT")))
                .andExpect(jsonPath("$.member.passwordChangeRequired").value(true))
                .andReturn().getResponse().getContentAsString();
        String temporaryPassword = JsonPath.read(body, "$.temporaryPassword");

        String temporaryToken = token(email, temporaryPassword);
        getWithToken("/api/admin/clients", temporaryToken).andExpect(status().isForbidden());
        String newToken = mvc.perform(post("/api/auth/password")
                        .header("Authorization", bearer(temporaryToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"" + temporaryPassword + "\", \"newPassword\": \"a-fresh-agent-pass-1\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(AuthCookies.SESSION_COOKIE).getValue();
        getWithToken("/api/admin/clients", newToken).andExpect(status().isOk());
    }

    @Test
    void staffListContainsOnlyThisOrganizationsStaff() throws Exception {
        String agent = newAgent(org);
        OnboardedClient client = onboardClient(token(agent, AGENT_PASSWORD));
        String outsider = newAgent(newOrganization());

        getWithToken("/api/org/staff", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", containsInAnyOrder(admin, agent)))
                .andExpect(content().string(not(containsString(outsider))))
                .andExpect(content().string(not(containsString(client.username()))))
                .andExpect(content().string(not(containsString("passwordHash"))));
    }

    @Test
    void disablingAnAgentEndsTheirAccessImmediately() throws Exception {
        String agent = newAgent(org);
        String agentToken = token(agent, AGENT_PASSWORD);

        setEnabled(adminToken, idOf(agent), false).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
        getWithToken("/api/admin/clients", agentToken).andExpect(status().isUnauthorized());

        setEnabled(adminToken, idOf(agent), true).andExpect(jsonPath("$.enabled").value(true));
        getWithToken("/api/admin/clients", agentToken).andExpect(status().isOk());

        getWithToken("/api/admin/audit-events?type=STAFF_ENABLED_CHANGED", adminToken)
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void anAdministratorCannotDisableThemselves() throws Exception {
        setEnabled(adminToken, idOf(admin), false)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CANNOT_CHANGE_OWN_ACCOUNT"));
    }

    /**
     * Through one request the "last administrator" rule cannot trigger: the caller is itself an active admin.
     * It matters when two admins disable each other at the same instant, both still authenticated as active.
     * The organization row lock serializes them; the second one sees a single active admin left and is refused.
     */
    @Test
    void twoAdministratorsDisablingEachOtherConcurrentlyLeaveOneActive() throws Exception {
        String other = newStaff(org, AppRole.ORG_ADMIN);
        var a = new AppUserPrincipal(idOf(admin), admin, org.getId());
        var b = new AppUserPrincipal(idOf(other), other, org.getId());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Object> aDisablesB = pool.submit(() -> attempt(start, a, b.userId()));
            Future<Object> bDisablesA = pool.submit(() -> attempt(start, b, a.userId()));
            start.countDown();
            List<Object> outcomes = List.of(aDisablesB.get(30, TimeUnit.SECONDS), bDisablesA.get(30, TimeUnit.SECONDS));

            assertThat(outcomes).filteredOn(o -> o instanceof OrganizationDtos.StaffMember).hasSize(1);
            assertThat(outcomes).filteredOn(o -> o instanceof BusinessRuleException e && e.getCode().equals("LAST_ORG_ADMIN"))
                    .hasSize(1);
            assertThat(userRepo.countEnabledWithRole(org.getId(), AppRole.ORG_ADMIN)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private Object attempt(CountDownLatch start, AppUserPrincipal actor, Long target) throws InterruptedException {
        start.await();
        try {
            return orgAdminService.setEnabled(actor, target, false);
        } catch (RuntimeException e) {
            return e;
        }
    }

    @Test
    void staffOfAnotherOrganizationIsNotFound() throws Exception {
        String outsider = newAgent(newOrganization());
        setEnabled(adminToken, idOf(outsider), false)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STAFF_NOT_FOUND"));
        getWithToken("/api/admin/clients", token(outsider, AGENT_PASSWORD)).andExpect(status().isOk());
    }

    @Test
    void clientsAreNotManagedAsStaff() throws Exception {
        OnboardedClient client = onboardClient(agentToken(org));
        setEnabled(adminToken, idOf(client.username()), false).andExpect(status().isNotFound());
    }

    @Test
    void invalidStaffRequestsAreRejected() throws Exception {
        createStaff(adminToken, "not-an-email", "PLATFORM_ADMIN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.role").exists());
        createStaff(adminToken, admin, "AGENT")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void agentsAndClientsCannotAdministerTheOrganization() throws Exception {
        String agentToken = agentToken(org);
        getWithToken("/api/org/overview", agentToken).andExpect(status().isForbidden());
        getWithToken("/api/org/staff", clientToken(onboardClient(agentToken))).andExpect(status().isForbidden());
    }

    @Test
    void orgAdminsReadTheAuditTrailButDoNotOperateAccounts() throws Exception {
        getWithToken("/api/admin/audit-events", adminToken).andExpect(status().isOk());
        getWithToken("/api/admin/clients", adminToken).andExpect(status().isForbidden());
    }
}
