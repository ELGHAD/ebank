package com.example.ebank.web;

import com.example.ebank.IntegrationTestSupport;
import com.example.ebank.entities.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Client search, client details and account status management for agents. */
class AgentWorkspaceIntegrationTest extends IntegrationTestSupport {

    Organization org;
    String agentToken;

    @BeforeEach
    void setUp() throws Exception {
        org = newOrganization();
        agentToken = agentToken(org);
    }

    private long onboard(String firstName, String lastName, String email, String identityRef) throws Exception {
        String body = mvc.perform(post("/api/admin/clients")
                        .header("Authorization", bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identityRef": "%s", "firstName": "%s", "lastName": "%s", "email": "%s"}"""
                                .formatted(identityRef, firstName, lastName, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.client.id")).longValue();
    }

    private ResultActions changeStatus(String token, long accountId, String status) throws Exception {
        return mvc.perform(patch("/api/admin/accounts/" + accountId + "/status")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"" + status + "\"}"));
    }

    @Test
    void searchListsOnlyTheOrganizationsClientsSortedWithAccountCounts() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 6);
        long zoe = onboard("Zoe", "Alami-" + tag, "zoe." + tag + "@test.ebank", "ID-Z-" + tag);
        onboard("Adam", "Bennani-" + tag, "adam." + tag + "@test.ebank", "ID-A-" + tag);
        newAccount(agentToken, zoe, "10");
        newAccount(agentToken, zoe, "0", "EUR");
        onboardClient(agentToken(newOrganization())); // another organization: never listed

        getWithToken("/api/admin/clients", agentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].lastName", contains("Alami-" + tag, "Bennani-" + tag)))
                .andExpect(jsonPath("$.content[0].accountCount").value(2))
                .andExpect(jsonPath("$.content[1].accountCount").value(0))
                .andExpect(jsonPath("$.content[0].email").value("zoe." + tag + "@test.ebank"));
    }

    @Test
    void searchMatchesNameEmailAndIdentityCaseInsensitively() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 6);
        onboard("Yasmine", "Idrissi", "yasmine." + tag + "@test.ebank", "CIN-" + tag);
        onboard("Karim", "Tazi", "karim." + tag + "@test.ebank", "PASS-" + tag);

        getWithToken("/api/admin/clients?q=yasMINE", agentToken).andExpect(jsonPath("$.totalElements").value(1));
        getWithToken("/api/admin/clients?q=TAZI", agentToken).andExpect(jsonPath("$.content[0].firstName").value("Karim"));
        getWithToken("/api/admin/clients?q=karim." + tag, agentToken).andExpect(jsonPath("$.totalElements").value(1));
        getWithToken("/api/admin/clients?q=cin-" + tag, agentToken).andExpect(jsonPath("$.content[0].lastName").value("Idrissi"));
        // LIKE wildcards typed by the agent are matched literally, not as "anything"
        getWithToken("/api/admin/clients?q=%25", agentToken).andExpect(jsonPath("$.totalElements").value(0));
        getWithToken("/api/admin/clients?q=_", agentToken).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void searchIsPaginated() throws Exception {
        for (int i = 0; i < 3; i++) {
            onboardClient(agentToken);
        }
        getWithToken("/api/admin/clients?size=2&page=1", agentToken)
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(2));
        getWithToken("/api/admin/clients?size=500", agentToken).andExpect(status().isBadRequest());
    }

    @Test
    void clientDetailsShowAccountsAndOnboardingState() throws Exception {
        OnboardedClient client = onboardClient(agentToken);
        AccountInfo account = newAccount(agentToken, client.id(), "42.50");

        getWithToken("/api/admin/clients/" + client.id(), agentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.client.email").value(client.username()))
                .andExpect(jsonPath("$.passwordChangeRequired").value(true))
                .andExpect(jsonPath("$.accounts[0].rib").value(account.rib()))
                .andExpect(jsonPath("$.accounts[0].balance").value(42.50))
                .andExpect(content().string(not(containsString("passwordHash"))));

        clientToken(client);
        getWithToken("/api/admin/clients/" + client.id(), agentToken)
                .andExpect(jsonPath("$.passwordChangeRequired").value(false));
    }

    @Test
    void clientsAndAccountsOfAnotherOrganizationAreNotFound() throws Exception {
        String otherAgent = agentToken(newOrganization());
        OnboardedClient outsider = onboardClient(otherAgent);
        AccountInfo outsiderAccount = newAccount(otherAgent, outsider.id(), "5");

        getWithToken("/api/admin/clients/" + outsider.id(), agentToken)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
        getWithToken("/api/admin/accounts/" + outsiderAccount.id(), agentToken)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
        changeStatus(agentToken, outsiderAccount.id(), "BLOCKED").andExpect(status().isNotFound());
        getWithToken("/api/admin/accounts/" + outsiderAccount.id(), otherAgent)
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void blockAndUnblockAnAccount() throws Exception {
        AccountInfo account = newAccount(agentToken, onboardClient(agentToken).id(), "10");

        changeStatus(agentToken, account.id(), "BLOCKED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BLOCKED"));
        changeStatus(agentToken, account.id(), "BLOCKED").andExpect(status().isOk()); // idempotent
        changeStatus(agentToken, account.id(), "ACTIVE").andExpect(jsonPath("$.status").value("ACTIVE"));

        getWithToken("/api/admin/audit-events?type=ACCOUNT_STATUS_CHANGED", agentToken)
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].details", contains("BLOCKED -> ACTIVE", "ACTIVE -> BLOCKED")));
    }

    @Test
    void closingRequiresAZeroBalanceAndIsFinal() throws Exception {
        AccountInfo funded = newAccount(agentToken, onboardClient(agentToken).id(), "10");
        changeStatus(agentToken, funded.id(), "CLOSED")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("BALANCE_NOT_ZERO"));

        AccountInfo empty = newAccount(agentToken, onboardClient(agentToken).id(), "0");
        changeStatus(agentToken, empty.id(), "CLOSED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
        changeStatus(agentToken, empty.id(), "ACTIVE")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ACCOUNT_CLOSED"));
    }

    @Test
    void invalidStatusIs400AndClientsCannotChangeStatuses() throws Exception {
        OnboardedClient client = onboardClient(agentToken);
        AccountInfo account = newAccount(agentToken, client.id(), "0");

        changeStatus(agentToken, account.id(), "FROZEN").andExpect(status().isBadRequest());
        changeStatus(clientToken(client), account.id(), "BLOCKED").andExpect(status().isForbidden());
    }
}
