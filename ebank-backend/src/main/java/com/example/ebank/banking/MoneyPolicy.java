package com.example.ebank.banking;

import com.example.ebank.config.BankingProperties;
import com.example.ebank.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Central rules for currencies and amounts. Amounts are never rounded silently:
 * an amount with more decimals than the currency allows is rejected.
 */
@Component
@RequiredArgsConstructor
public class MoneyPolicy {

    private final BankingProperties properties;

    /** Returns the normalized currency code, or the default currency when none is given. */
    public String resolveCurrency(String requested) {
        if (requested == null || requested.isBlank()) {
            return properties.defaultCurrency();
        }
        String code = requested.trim().toUpperCase();
        if (!properties.supportedCurrencies().contains(code)) {
            throw new BadRequestException("UNSUPPORTED_CURRENCY",
                    "Currency " + code + " is not supported. Supported: " + properties.supportedCurrencies());
        }
        return code;
    }

    /** Validates a non-negative amount for the currency and returns it at the currency's scale. */
    public BigDecimal requireNonNegativeAmount(BigDecimal amount, String currency) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(scaleOf(currency));
        }
        if (amount.signum() < 0) {
            throw new BadRequestException("NEGATIVE_AMOUNT", "Amount must not be negative");
        }
        return normalize(amount, currency);
    }

    private BigDecimal normalize(BigDecimal amount, String currency) {
        int scale = scaleOf(currency);
        if (amount.stripTrailingZeros().scale() > scale) {
            throw new BadRequestException("INVALID_AMOUNT_SCALE",
                    "Amount has more than " + scale + " decimal places for " + currency);
        }
        return amount.setScale(scale);
    }

    private static int scaleOf(String currency) {
        return Currency.getInstance(currency).getDefaultFractionDigits();
    }
}
