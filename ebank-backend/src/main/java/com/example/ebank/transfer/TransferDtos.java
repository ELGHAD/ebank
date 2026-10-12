package com.example.ebank.transfer;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/** Request and response payloads of the transfer and ledger API. */
public final class TransferDtos {

    private TransferDtos() {
    }

    public record TransferRequest(
            @NotNull(message = "Source account is required") Long sourceAccountId,
            @NotBlank(message = "Recipient RIB is required") String destinationRib,
            @NotNull(message = "Amount is required")
            @Positive(message = "Amount must be greater than zero")
            @DecimalMax(value = "1000000000", message = "Amount is too large") BigDecimal amount,
            @Size(max = 140, message = "Description must be at most 140 characters") String description) {
    }

    /**
     * @param balanceAfter the source account balance right after this transfer
     */
    public record TransferResponse(Long id, Long sourceAccountId, String sourceRib, String destinationRib,
                                   BigDecimal amount, String currency, String description,
                                   BigDecimal balanceAfter, Instant createdAt) {
    }

    /**
     * @param counterpartyRib the other account of the transfer; null for deposits
     */
    public record LedgerEntryResponse(Long id, LedgerEntryType type, BigDecimal amount, BigDecimal balanceAfter,
                                      String description, Long transferId, String counterpartyRib,
                                      Instant createdAt) {
    }
}
