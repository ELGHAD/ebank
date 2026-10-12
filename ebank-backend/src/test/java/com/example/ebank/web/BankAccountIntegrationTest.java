package com.example.ebank.web;

import com.example.ebank.IntegrationTestSupport;
import com.example.ebank.banking.Rib;
import com.example.ebank.entities.Organization;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BankAccountIntegrationTest extends IntegrationTestSupport {

    @Test
    void agentOpensAccountWithServerGeneratedRibAndExactBalance() throws Exception {
        String agentToken = agentToken(newOrganization());
        OnboardedClient client = onboardClient(agentToken);

        String body = openAccount(agentToken, client.id(), "\"initialDeposit\": 1500.5")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("MAD"))
                .andExpect(jsonPath("$.balance").value(1500.50))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.clientId").value(client.id()))
                .andReturn().getResponse().getContentAsString();

        String rib = JsonPath.read(body, "$.rib");
        assertThat(Rib.isValid(rib)).as("generated RIB %s has valid check digits", rib).isTrue();
    }

    @Test
    void ribSuppliedByTheCallerIsIgnored() throws Exception {
        String agentToken = agentToken(newOrganization());
        OnboardedClient client = onboardClient(agentToken);

        openAccount(agentToken, client.id(), "\"rib\": \"EB0000000000000000000000\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rib").value(not("EB0000000000000000000000")));
    }

    @Test
    void currencyDefaultsAndUnsupportedCurrencyIsRejected() throws Exception {
        String agentToken = agentToken(newOrganization());
        OnboardedClient client = onboardClient(agentToken);

        openAccount(agentToken, client.id(), "\"currency\": \"eur\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balance").value(0));
        openAccount(agentToken, client.id(), "\"currency\": \"JPY\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_CURRENCY"));
    }

    // Regression for audit D-6/D-14: negative initial balances were accepted
    @Test
    void negativeOrOverPreciseDepositsAreRejected() throws Exception {
        String agentToken = agentToken(newOrganization());
        OnboardedClient client = onboardClient(agentToken);

        openAccount(agentToken, client.id(), "\"initialDeposit\": -500")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.initialDeposit").exists());
        openAccount(agentToken, client.id(), "\"initialDeposit\": 10.005")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AMOUNT_SCALE"));
    }

    @Test
    void unknownOrMissingClientIsRejected() throws Exception {
        String agentToken = agentToken(newOrganization());
        openAccount(agentToken, 999_999_999L, "")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/admin/accounts")
                        .header("Authorization", bearer(agentToken))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.clientId").exists());
    }

    // Regression for audit S-1: account listings exposed the owner's password hash and roles
    @Test
    void clientSeesOnlyOwnAccountsAsDtosWithoutSecrets() throws Exception {
        String agentToken = agentToken(newOrganization());
        OnboardedClient alice = onboardClient(agentToken);
        OnboardedClient bob = onboardClient(agentToken);
        openAccount(agentToken, alice.id(), "\"initialDeposit\": 100").andExpect(status().isCreated());
        openAccount(agentToken, alice.id(), "\"initialDeposit\": 0.1").andExpect(status().isCreated());
        openAccount(agentToken, bob.id(), "\"initialDeposit\": 999").andExpect(status().isCreated());

        getWithToken("/api/client/accounts", token(alice.username(), alice.temporaryPassword()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].clientId", everyItem(is((int) alice.id()))))
                .andExpect(jsonPath("$[0].balance").value(100.00))
                .andExpect(jsonPath("$[1].balance").value(0.10))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("roles"))));
    }

    @Test
    void agentListsAccountsOfOwnClientsWithoutSecrets() throws Exception {
        String agentToken = agentToken(newOrganization());
        OnboardedClient client = onboardClient(agentToken);
        openAccount(agentToken, client.id(), "").andExpect(status().isCreated());

        getWithToken("/api/admin/clients/" + client.id() + "/accounts", agentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(content().string(not(containsString("password"))));
    }

    // Tenant isolation: an agent of organization B cannot see or use organization A's clients
    @Test
    void agentCannotReachClientsOfAnotherOrganization() throws Exception {
        Organization orgA = newOrganization();
        Organization orgB = newOrganization();
        String agentA = agentToken(orgA);
        String agentB = agentToken(orgB);
        OnboardedClient clientOfA = onboardClient(agentA);

        getWithToken("/api/admin/clients/" + clientOfA.id() + "/accounts", agentB)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
        openAccount(agentB, clientOfA.id(), "\"initialDeposit\": 1000")
                .andExpect(status().isNotFound());

        getWithToken("/api/admin/clients/" + clientOfA.id() + "/accounts", agentA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
