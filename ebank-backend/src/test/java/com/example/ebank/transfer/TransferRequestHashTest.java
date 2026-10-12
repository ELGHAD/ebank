package com.example.ebank.transfer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransferRequestHashTest {

    @Test
    void equivalentRequestsHashTheSame() {
        assertThat(TransferService.hash(1L, "EB00", new BigDecimal("10.5"), " Rent "))
                .isEqualTo(TransferService.hash(1L, "EB00", new BigDecimal("10.50"), "Rent"));
    }

    @Test
    void anyMeaningfulDifferenceChangesTheHash() {
        String base = TransferService.hash(1L, "EB00", new BigDecimal("10"), "Rent");
        assertThat(TransferService.hash(2L, "EB00", new BigDecimal("10"), "Rent")).isNotEqualTo(base);
        assertThat(TransferService.hash(1L, "EB01", new BigDecimal("10"), "Rent")).isNotEqualTo(base);
        assertThat(TransferService.hash(1L, "EB00", new BigDecimal("10.01"), "Rent")).isNotEqualTo(base);
        assertThat(TransferService.hash(1L, "EB00", new BigDecimal("10"), "Food")).isNotEqualTo(base);
    }
}
