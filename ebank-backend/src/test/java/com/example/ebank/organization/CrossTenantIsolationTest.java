package com.example.ebank.organization;

import com.example.ebank.IntegrationTestSupport;
import com.example.ebank.entities.AppRole;
import com.example.ebank.entities.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Systematic tenant isolation check: users of organization A call every tenant-scoped endpoint with the
 * identifiers of organization B's resources. Every call must be refused (404, or 403 for the wrong role) and
 * organization B's data must be unchanged. A second test shows the database refuses cross-tenant rows itself.
 */
class CrossTenantIsolationTest extends IntegrationTestSupport {

    Organization orgA;
    Organization orgB;
    String agentA;
    String clientA;
    String adminA;
    OnboardedClient clientOfB;
    AccountInfo accountOfB;
    AccountInfo accountOfA;
    long staffOfB;

    @BeforeEach
    void setUp() throws Exception {
        orgA = newOrganization();
        orgB = newOrganization();
        agentA = agentToken(orgA);
        adminA = staffToken(orgA, AppRole.ORG_ADMIN);
        OnboardedClient a = onboardClient(agentA);
        accountOfA = newAccount(agentA, a.id(), "100");
        clientA = clientToken(a);

        String agentB = agentToken(orgB);
        clientOfB = onboardClient(agentB);
        accountOfB = newAccount(agentB, clientOfB.id(), "500");
        staffOfB = userRepo.findByUsername(newAgent(orgB)).orElseThrow().getId();
    }

    private record Call(String who, String token, HttpMethod method, String url, String body) {
    }

    @Test
    void everyTenantScopedEndpointRefusesAnotherOrganizationsIds() throws Exception {
        long b = accountOfB.id();
        List<Call> calls = List.of(
                new Call("agent", agentA, HttpMethod.GET, "/api/admin/clients/" + clientOfB.id(), null),
                new Call("agent", agentA, HttpMethod.GET, "/api/admin/clients/" + clientOfB.id() + "/accounts", null),
                new Call("agent", agentA, HttpMethod.GET, "/api/admin/accounts/" + b, null),
                new Call("agent", agentA, HttpMethod.GET, "/api/admin/accounts/" + b + "/entries", null),
                new Call("agent", agentA, HttpMethod.PATCH, "/api/admin/accounts/" + b + "/status", "{\"status\": \"BLOCKED\"}"),
                new Call("agent", agentA, HttpMethod.POST, "/api/admin/accounts",
                        "{\"clientId\": " + clientOfB.id() + ", \"initialDeposit\": 1}"),
                new Call("client", clientA, HttpMethod.GET, "/api/client/accounts/" + b + "/entries", null),
                new Call("client", clientA, HttpMethod.POST, "/api/client/transfers",
                        "{\"sourceAccountId\": " + b + ", \"destinationRib\": \"" + accountOfA.rib() + "\", \"amount\": 1}"),
                new Call("client", clientA, HttpMethod.POST, "/api/client/transfers",
                        "{\"sourceAccountId\": " + accountOfA.id() + ", \"destinationRib\": \"" + accountOfB.rib() + "\", \"amount\": 1}"),
                new Call("org admin", adminA, HttpMethod.PATCH, "/api/org/staff/" + staffOfB + "/enabled", "{\"enabled\": false}"));

        List<String> leaks = new ArrayList<>();
        for (Call call : calls) {
            var request = request(call.method(), call.url())
                    .header("Authorization", bearer(call.token()))
                    .header("Idempotency-Key", UUID.randomUUID().toString());
            if (call.body() != null) {
                request.contentType(MediaType.APPLICATION_JSON).content(call.body());
            }
            int status = mvc.perform(request).andReturn().getResponse().getStatus();
            if (status != 404 && status != 403) {
                leaks.add(call.who() + " " + call.method() + " " + call.url() + " -> " + status);
            }
        }

        assertThat(leaks).as("calls that were not refused").isEmpty();
        // Nothing of organization B changed
        assertThat(balanceOf(accountOfB.id())).isEqualByComparingTo("500.00");
        assertThat(balanceOf(accountOfA.id())).isEqualByComparingTo("100.00");
        assertThat(jdbc.queryForObject("select status from bank_account where id = ?", String.class, b)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("select enabled from app_user where id = ?", Boolean.class, staffOfB)).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from bank_account where client_id = ?", Long.class, clientOfB.id()))
                .isEqualTo(1);
    }

    @Test
    void listingsNeverIncludeAnotherOrganizationsData() throws Exception {
        String clients = getWithToken("/api/admin/clients?size=100", agentA).andReturn().getResponse().getContentAsString();
        String audit = getWithToken("/api/admin/audit-events?size=100", adminA).andReturn().getResponse().getContentAsString();
        String staff = getWithToken("/api/org/staff", adminA).andReturn().getResponse().getContentAsString();
        String accounts = getWithToken("/api/client/accounts", clientA).andReturn().getResponse().getContentAsString();

        assertThat(clients).doesNotContain(clientOfB.username());
        assertThat(audit).doesNotContain(clientOfB.username()).doesNotContain("account:" + accountOfB.id() + "\"");
        assertThat(staff).doesNotContain("\"id\":" + staffOfB + ",");
        assertThat(accounts).doesNotContain(accountOfB.rib());
    }

    /** Defence in depth: even code that bypasses the services (here: the schema owner) cannot mix tenants. */
    @Test
    void theDatabaseRejectsCrossTenantRows() {
        var owner = ownerJdbc();
        Long userOfB = jdbc.queryForObject("select id from app_user where username = ?", Long.class, clientOfB.username());

        // A ledger entry in organization A for an account of organization B
        assertThatThrownBy(() -> owner.update("""
                insert into ledger_entry (organization_id, account_id, entry_type, amount, balance_after)
                values (?, ?, 'DEPOSIT', 1, 1)""", orgA.getId(), accountOfB.id()))
                .rootCause().hasMessageContaining("fk_ledger_entry_account_same_org");

        // A transfer of organization A whose destination is an account of organization B
        assertThatThrownBy(() -> owner.update("""
                insert into transfer (organization_id, initiated_by_id, idempotency_key, request_hash,
                                      source_account_id, destination_account_id, amount, currency)
                values (?, ?, 'k-cross-tenant', 'h', ?, ?, 1, 'MAD')""",
                orgA.getId(), userOfB, accountOfA.id(), accountOfB.id()))
                .rootCause().hasMessageContaining("fk_transfer_destination_same_org");

        // A client of organization A whose login belongs to organization B
        assertThatThrownBy(() -> owner.update("""
                insert into client (organization_id, app_user_id, identity_ref, first_name, last_name)
                values (?, ?, 'X-CROSS', 'X', 'Y')""", orgA.getId(), staffOfB)) // a user of B without a client record
                .rootCause().hasMessageContaining("fk_client_app_user_same_org");
    }
}
