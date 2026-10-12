package com.example.ebank.banking;

import com.example.ebank.config.BankingProperties;
import com.example.ebank.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyPolicyTest {

    private final MoneyPolicy policy = new MoneyPolicy(new BankingProperties("MAD", List.of("MAD", "EUR")));

    @Test
    void resolvesDefaultAndNormalizesCase() {
        assertThat(policy.resolveCurrency(null)).isEqualTo("MAD");
        assertThat(policy.resolveCurrency(" eur ")).isEqualTo("EUR");
    }

    @Test
    void rejectsUnsupportedCurrency() {
        assertThatThrownBy(() -> policy.resolveCurrency("USD"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("USD");
    }

    @Test
    void normalizesToCurrencyScaleWithoutRounding() {
        assertThat(policy.requireNonNegativeAmount(new BigDecimal("10.5"), "MAD")).isEqualTo(new BigDecimal("10.50"));
        assertThat(policy.requireNonNegativeAmount(new BigDecimal("10.500"), "MAD")).isEqualTo(new BigDecimal("10.50"));
        assertThat(policy.requireNonNegativeAmount(null, "MAD")).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void rejectsNegativeAndOverPreciseAmounts() {
        assertThatThrownBy(() -> policy.requireNonNegativeAmount(new BigDecimal("-0.01"), "MAD"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> policy.requireNonNegativeAmount(new BigDecimal("0.001"), "MAD"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("2 decimal places");
    }

    @Test
    void decimalArithmeticIsExact() {
        // The reason money is BigDecimal: 0.1 + 0.2 == 0.30000000000000004 with double
        assertThat(new BigDecimal("0.10").add(new BigDecimal("0.20"))).isEqualByComparingTo("0.30");
    }
}
