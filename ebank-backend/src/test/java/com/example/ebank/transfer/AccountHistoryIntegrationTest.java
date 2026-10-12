package com.example.ebank.transfer;

import com.example.ebank.IntegrationTestSupport;
import com.example.ebank.entities.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountHistoryIntegrationTest extends IntegrationTestSupport {

    Organization org;
    String agentToken;
    String aliceToken;
    AccountInfo alice;
    AccountInfo bob;

    @BeforeEach
    void setUp() throws Exception {
        org = newOrganization();
        agentToken = agentToken(org);
        OnboardedClient aliceClient = onboardClient(agentToken);
        aliceToken = token(aliceClient.username(), aliceClient.temporaryPassword());
        alice = newAccount(agentToken, aliceClient.id(), "100.00");
        bob = newAccount(agentToken, onboardClient(agentToken).id(), "0");

        // Opening deposit, then three transfers out: 10, 20, 30
        for (int amount : new int[]{10, 20, 30}) {
            mvc.perform(post("/api/client/transfers")
                            .header("Authorization", bearer(aliceToken))
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"sourceAccountId": %d, "destinationRib": "%s", "amount": %d, "description": "Payment %d"}"""
                                    .formatted(alice.id(), bob.rib(), amount, amount)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void historyIsNewestFirstWithRunningBalanceAndCounterparty() throws Exception {
        getWithToken("/api/client/accounts/" + alice.id() + "/entries", aliceToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[*].type", contains("DEBIT", "DEBIT", "DEBIT", "DEPOSIT")))
                .andExpect(jsonPath("$.content[*].amount", contains(30.0, 20.0, 10.0, 100.0)))
                .andExpect(jsonPath("$.content[*].balanceAfter", contains(40.0, 70.0, 90.0, 100.0)))
                .andExpect(jsonPath("$.content[0].counterpartyRib").value(bob.rib()))
                .andExpect(jsonPath("$.content[0].description").value("Payment 30"))
                .andExpect(jsonPath("$.content[3].counterpartyRib").doesNotExist());
    }

    @Test
    void recipientSeesCreditsWithTheSenderAsCounterparty() throws Exception {
        getWithToken("/api/admin/accounts/" + bob.id() + "/entries", agentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].type", everyItem(is("CREDIT"))))
                .andExpect(jsonPath("$.content[0].counterpartyRib").value(alice.rib()));
    }

    @Test
    void historyIsPaginated() throws Exception {
        getWithToken("/api/client/accounts/" + alice.id() + "/entries?size=3&page=1", aliceToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void historyCanBeFilteredByTypeAndDate() throws Exception {
        getWithToken("/api/client/accounts/" + alice.id() + "/entries?type=DEPOSIT", aliceToken)
                .andExpect(jsonPath("$.totalElements").value(1));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        getWithToken("/api/client/accounts/" + alice.id() + "/entries?from=" + today + "&to=" + today, aliceToken)
                .andExpect(jsonPath("$.totalElements").value(4));
        getWithToken("/api/client/accounts/" + alice.id() + "/entries?to=" + today.minusDays(1), aliceToken)
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidPagingOrFilterIs400() throws Exception {
        getWithToken("/api/client/accounts/" + alice.id() + "/entries?size=101", aliceToken)
                .andExpect(status().isBadRequest());
        getWithToken("/api/client/accounts/" + alice.id() + "/entries?page=-1", aliceToken)
                .andExpect(status().isBadRequest());
        getWithToken("/api/client/accounts/" + alice.id() + "/entries?type=REFUND", aliceToken)
                .andExpect(status().isBadRequest());
    }

    // Object-level authorization on history
    @Test
    void clientCannotReadSomeoneElsesHistory() throws Exception {
        getWithToken("/api/client/accounts/" + bob.id() + "/entries", aliceToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void agentCannotReadHistoryOfAnotherOrganization() throws Exception {
        getWithToken("/api/admin/accounts/" + alice.id() + "/entries", agentToken(newOrganization()))
                .andExpect(status().isNotFound());
    }

    @Test
    void historyMatchesTheBalance() throws Exception {
        getWithToken("/api/client/accounts", aliceToken)
                .andExpect(jsonPath("$[0].balance").value(40.0));
        assertBooksBalanced();
    }
}
