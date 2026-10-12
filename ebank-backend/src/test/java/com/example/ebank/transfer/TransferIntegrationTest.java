package com.example.ebank.transfer;

import com.example.ebank.IntegrationTestSupport;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.entities.AccountStatus;
import com.example.ebank.entities.Organization;
import com.example.ebank.repositories.BankAccountRepository;
import com.example.ebank.transfer.TransferDtos.TransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransferIntegrationTest extends IntegrationTestSupport {

    @Autowired BankAccountRepository accountRepo;
    @Autowired TransferService transferService;

    String agentToken;
    OnboardedClient alice;
    String aliceToken;
    AccountInfo aliceMad;   // 1000.00 MAD
    AccountInfo bobMad;     // 50.00 MAD
    Organization org;

    @BeforeEach
    void setUp() throws Exception {
        org = newOrganization();
        agentToken = agentToken(org);
        alice = onboardClient(agentToken);
        aliceToken = token(alice.username(), alice.temporaryPassword());
        OnboardedClient bob = onboardClient(agentToken);
        aliceMad = newAccount(agentToken, alice.id(), "1000.00");
        bobMad = newAccount(agentToken, bob.id(), "50.00");
    }

    private ResultActions transfer(String token, String key, long sourceId, String destinationRib, String amount)
            throws Exception {
        var request = post("/api/client/transfers")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sourceAccountId": %d, "destinationRib": "%s", "amount": %s, "description": "Rent"}"""
                        .formatted(sourceId, destinationRib, amount));
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return mvc.perform(request);
    }

    private ResultActions transfer(long sourceId, String destinationRib, String amount) throws Exception {
        return transfer(aliceToken, UUID.randomUUID().toString(), sourceId, destinationRib, amount);
    }

    @Test
    void successfulTransferMovesMoneyAndWritesBalancedLedgerEntries() throws Exception {
        transfer(aliceMad.id(), bobMad.rib(), "250.50")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount").value(250.50))
                .andExpect(jsonPath("$.currency").value("MAD"))
                .andExpect(jsonPath("$.sourceRib").value(aliceMad.rib()))
                .andExpect(jsonPath("$.destinationRib").value(bobMad.rib()))
                .andExpect(jsonPath("$.balanceAfter").value(749.50));

        assertThat(balanceOf(aliceMad.id())).isEqualByComparingTo("749.50");
        assertThat(balanceOf(bobMad.id())).isEqualByComparingTo("300.50");
        assertBooksBalanced();
    }

    @Test
    void recipientRibIsAcceptedWithSurroundingSpacesAndLowercase() throws Exception {
        transfer(aliceMad.id(), "  " + bobMad.rib().toLowerCase() + " ", "1").andExpect(status().isCreated());
    }

    @Test
    void insufficientFundsIsRejectedWithoutAnyEffect() throws Exception {
        transfer(aliceMad.id(), bobMad.rib(), "1000.01")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));

        assertThat(balanceOf(aliceMad.id())).isEqualByComparingTo("1000.00");
        assertThat(balanceOf(bobMad.id())).isEqualByComparingTo("50.00");
        assertThat(jdbc.queryForObject("select count(*) from transfer where source_account_id = ?", Long.class,
                aliceMad.id())).isZero();
    }

    @Test
    void wholeBalanceCanBeTransferred() throws Exception {
        transfer(aliceMad.id(), bobMad.rib(), "1000").andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter").value(0));
    }

    @Test
    void nonPositiveOrOverPreciseAmountsAreRejected() throws Exception {
        transfer(aliceMad.id(), bobMad.rib(), "0").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
        transfer(aliceMad.id(), bobMad.rib(), "-5").andExpect(status().isBadRequest());
        transfer(aliceMad.id(), bobMad.rib(), "10.001").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AMOUNT_SCALE"));
    }

    @Test
    void mistypedRibIsRejectedBeforeAnyLookup() throws Exception {
        String rib = bobMad.rib();
        char last = rib.charAt(rib.length() - 1);
        String typo = rib.substring(0, rib.length() - 1) + (last == '9' ? '0' : (char) (last + 1));
        transfer(aliceMad.id(), typo, "1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RIB"));
    }

    @Test
    void unknownRecipientIs404() throws Exception {
        String unused = com.example.ebank.banking.Rib.generate();
        transfer(aliceMad.id(), unused, "1")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPIENT_NOT_FOUND"));
    }

    @Test
    void sameAccountIsRejected() throws Exception {
        transfer(aliceMad.id(), aliceMad.rib(), "1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SAME_ACCOUNT"));
    }

    @Test
    void transferBetweenOwnAccountsIsAllowed() throws Exception {
        AccountInfo aliceSavings = newAccount(agentToken, alice.id(), "0");
        transfer(aliceMad.id(), aliceSavings.rib(), "100").andExpect(status().isCreated());
        assertThat(balanceOf(aliceSavings.id())).isEqualByComparingTo("100.00");
    }

    @Test
    void currencyMismatchIsRejected() throws Exception {
        OnboardedClient carol = onboardClient(agentToken);
        AccountInfo carolEur = newAccount(agentToken, carol.id(), "10", "EUR");
        transfer(aliceMad.id(), carolEur.rib(), "1")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CURRENCY_MISMATCH"));
    }

    @Test
    void blockedOrClosedAccountsCannotSendOrReceive() throws Exception {
        setStatus(bobMad.id(), AccountStatus.BLOCKED);
        transfer(aliceMad.id(), bobMad.rib(), "1")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("RECIPIENT_ACCOUNT_NOT_ACTIVE"));

        setStatus(bobMad.id(), AccountStatus.ACTIVE);
        setStatus(aliceMad.id(), AccountStatus.CLOSED);
        transfer(aliceMad.id(), bobMad.rib(), "1")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SOURCE_ACCOUNT_NOT_ACTIVE"));
    }

    // Object-level authorization: changing the source account id must not allow spending someone else's money
    @Test
    void cannotDebitSomeoneElsesAccount() throws Exception {
        transfer(aliceToken, UUID.randomUUID().toString(), bobMad.id(), aliceMad.rib(), "10")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SOURCE_ACCOUNT_NOT_FOUND"));
        assertThat(balanceOf(bobMad.id())).isEqualByComparingTo("50.00");
    }

    // Tenant isolation: accounts of another organization are invisible
    @Test
    void cannotTransferToAnotherOrganization() throws Exception {
        String otherAgent = agentToken(newOrganization());
        OnboardedClient outsider = onboardClient(otherAgent);
        AccountInfo outsiderAccount = newAccount(otherAgent, outsider.id(), "0");

        transfer(aliceMad.id(), outsiderAccount.rib(), "10")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPIENT_NOT_FOUND"));
        assertThat(balanceOf(outsiderAccount.id())).isEqualByComparingTo("0.00");
    }

    @Test
    void agentsCannotInitiateClientTransfers() throws Exception {
        transfer(agentToken, UUID.randomUUID().toString(), aliceMad.id(), bobMad.rib(), "1")
                .andExpect(status().isForbidden());
    }

    @Test
    void idempotencyKeyIsRequiredAndValidated() throws Exception {
        transfer(aliceToken, null, aliceMad.id(), bobMad.rib(), "1")
                .andExpect(status().isBadRequest());
        transfer(aliceToken, "short", aliceMad.id(), bobMad.rib(), "1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
    }

    @Test
    void retryWithSameKeyReplaysTheOriginalTransferWithoutChargingTwice() throws Exception {
        String key = UUID.randomUUID().toString();
        String firstBody = transfer(aliceToken, key, aliceMad.id(), bobMad.rib(), "100")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String replayBody = transfer(aliceToken, key, aliceMad.id(), bobMad.rib(), "100.00")
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andReturn().getResponse().getContentAsString();

        assertThat(replayBody).isEqualTo(firstBody);
        assertThat(balanceOf(aliceMad.id())).isEqualByComparingTo("900.00");
        assertThat(balanceOf(bobMad.id())).isEqualByComparingTo("150.00");
    }

    @Test
    void reusingAKeyForADifferentTransferIsRejected() throws Exception {
        String key = UUID.randomUUID().toString();
        transfer(aliceToken, key, aliceMad.id(), bobMad.rib(), "100").andExpect(status().isCreated());

        transfer(aliceToken, key, aliceMad.id(), bobMad.rib(), "200")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
        assertThat(balanceOf(aliceMad.id())).isEqualByComparingTo("900.00");
    }

    @Test
    void sameKeyFromDifferentUsersAreIndependentTransfers() throws Exception {
        OnboardedClient bob = onboardClient(agentToken);
        AccountInfo bobOwn = newAccount(agentToken, bob.id(), "10");
        String bobToken = token(bob.username(), bob.temporaryPassword());
        String key = UUID.randomUUID().toString();

        transfer(aliceToken, key, aliceMad.id(), bobMad.rib(), "1").andExpect(status().isCreated());
        transfer(bobToken, key, bobOwn.id(), aliceMad.rib(), "1").andExpect(status().isCreated());
    }

    // Atomicity: a failure after the debit has been written to the database leaves no trace
    @Test
    void failureAfterTheDebitRollsEverythingBack() {
        Long aliceUserId = userRepo.findByUsername(alice.username()).orElseThrow().getId();
        var caller = new AppUserPrincipal(aliceUserId, alice.username(), org.getId());
        // Bypasses API validation on purpose: transfer.description is varchar(140), so the transfer insert
        // fails after the balance updates have already been flushed
        var request = new TransferRequest(aliceMad.id(), bobMad.rib(), new BigDecimal("10"), "x".repeat(141));

        assertThatThrownBy(() -> transferService.transfer(caller, UUID.randomUUID().toString(), request))
                .isInstanceOf(Exception.class);

        assertThat(balanceOf(aliceMad.id())).isEqualByComparingTo("1000.00");
        assertThat(balanceOf(bobMad.id())).isEqualByComparingTo("50.00");
        assertBooksBalanced();
    }

    // The ledger cannot be rewritten, even by code that bypasses the application
    @Test
    void ledgerAndTransfersAreAppendOnlyInTheDatabase() throws Exception {
        transfer(aliceMad.id(), bobMad.rib(), "5").andExpect(status().isCreated());

        assertThatThrownBy(() -> jdbc.update("update ledger_entry set amount = 1 where account_id = ?", aliceMad.id()))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from transfer where source_account_id = ?", aliceMad.id()))
                .hasMessageContaining("append-only");
    }

    private void setStatus(long accountId, AccountStatus status) {
        var account = accountRepo.findById(accountId).orElseThrow();
        account.setStatus(status);
        accountRepo.save(account);
    }
}
