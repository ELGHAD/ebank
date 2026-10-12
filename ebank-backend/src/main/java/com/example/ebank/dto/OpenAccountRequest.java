package com.example.ebank.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * @param currency       ISO 4217 code; the organization's default currency when omitted
 * @param initialDeposit opening balance (simulated funds), zero when omitted
 */
public record OpenAccountRequest(
        @NotNull(message = "Client id is required") Long clientId,
        String currency,
        @PositiveOrZero(message = "Initial deposit must not be negative")
        @DecimalMax(value = "1000000000", message = "Initial deposit is too large") BigDecimal initialDeposit) {
}
