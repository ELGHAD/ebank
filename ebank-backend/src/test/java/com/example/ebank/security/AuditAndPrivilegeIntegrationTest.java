package com.example.ebank.security;

import com.example.ebank.IntegrationTestSupport;
import com.example.ebank.entities.Organization;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditAndPrivilegeIntegrationTest extends IntegrationTestSupport {

    @Test
    void businessAndSecurityEventsAreAuditedPerOrganization() throws Exception {
        Organization org = newOrganization();
        String agent = newAgent(org);
        login(agent, "wrong-password").andExpect(status().isUnauthorized());
        String agentToken = token(agent, AGENT_PASSWORD);
        OnboardedClient alice = onboardClient(agentToken);
        AccountInfo aliceAccount = newAccount(agentToken, alice.id(), "100");
        AccountInfo bobAccount = newAccount(agentToken, onboardClient(agentToken).id(), "0");
        String aliceToken = clientToken(alice);
        mvc.perform(post("/api/client/transfers")
                        .header("Authorization", bearer(aliceToken))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountId": %d, "destinationRib": "%s", "amount": 25}"""
                                .formatted(aliceAccount.id(), bobAccount.rib())))
                .andExpect(status().isCreated());

        String body = getWithToken("/api/admin/audit-events?size=100", agentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].type", hasItems(
                        "LOGIN_FAILED", "LOGIN_SUCCEEDED", "CLIENT_ONBOARDED", "ACCOUNT_OPENED",
                        "PASSWORD_CHANGED", "TRANSFER_COMPLETED")))
                .andExpect(jsonPath("$.content[?(@.type == 'TRANSFER_COMPLETED')].actorUsername", contains(alice.username())))
                .andExpect(jsonPath("$.content[?(@.type == 'LOGIN_FAILED')].ipAddress", contains("127.0.0.1")))
                .andReturn().getResponse().getContentAsString();

        // Never a secret in the trail
        assertThat(body).doesNotContain(alice.temporaryPassword()).doesNotContain(CLIENT_PASSWORD)
                .doesNotContain(AGENT_PASSWORD).doesNotContain("wrong-password");

        getWithToken("/api/admin/audit-events?type=ACCOUNT_OPENED", agentToken)
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void agentsOnlySeeTheirOwnOrganizationsAuditTrail() throws Exception {
        String agentA = agentToken(newOrganization());
        onboardClient(agentA);

        getWithToken("/api/admin/audit-events", agentToken(newOrganization()))
                .andExpect(jsonPath("$.content[*].type", not(hasItem("CLIENT_ONBOARDED"))));
    }

    @Test
    void clientsCannotReadTheAuditTrail() throws Exception {
        String clientToken = clientToken(onboardClient(agentToken(newOrganization())));
        getWithToken("/api/admin/audit-events", clientToken).andExpect(status().isForbidden());
    }

    // Least privilege: even with SQL injection, the application's database role could not change the schema
    // or rewrite history
    @Test
    void applicationDatabaseRoleHasNoDdlOrHistoryRewritePrivileges() {
        assertThat(jdbc.queryForObject("select current_user", String.class)).isEqualTo("ebank_app");

        // Spring wraps PostgreSQL's error (SQLSTATE 42501) in a generic exception: the reason is the root cause
        assertThatThrownBy(() -> jdbc.execute("drop table ledger_entry"))
                .rootCause().hasMessageContaining("must be owner");
        assertThatThrownBy(() -> jdbc.execute("alter table ledger_entry disable trigger all"))
                .rootCause().hasMessageContaining("must be owner");
        assertThatThrownBy(() -> jdbc.execute("create table hacked (id int)"))
                .rootCause().hasMessageContaining("permission denied");
        assertThatThrownBy(() -> jdbc.update("delete from audit_event"))
                .rootCause().hasMessageContaining("permission denied");
        assertThatThrownBy(() -> jdbc.queryForList("select * from flyway_schema_history"))
                .rootCause().hasMessageContaining("permission denied");
    }
}
