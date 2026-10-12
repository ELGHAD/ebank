package com.example.ebank.banking;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.assertj.core.api.Assertions.assertThat;

class RibTest {

    @RepeatedTest(50)
    void generatedRibsHaveTheExpectedFormatAndValidCheckDigits() {
        String rib = Rib.generate();
        assertThat(rib).matches("EB\\d{22}");
        assertThat(Rib.isValid(rib)).isTrue();
    }

    @Test
    void anySingleDigitTypoIsDetected() {
        String rib = Rib.generate(new SplittableRandom(42));
        for (int i = 2; i < rib.length(); i++) {
            char original = rib.charAt(i);
            char typo = original == '9' ? '0' : (char) (original + 1);
            String mistyped = rib.substring(0, i) + typo + rib.substring(i + 1);
            assertThat(Rib.isValid(mistyped)).as("typo at position %d", i).isFalse();
        }
    }

    @Test
    void malformedValuesAreInvalid() {
        assertThat(Rib.isValid(null)).isFalse();
        assertThat(Rib.isValid("")).isFalse();
        assertThat(Rib.isValid("FR7612345678901234567890")).isFalse();
        assertThat(Rib.isValid("EB12")).isFalse();
        assertThat(Rib.isValid("EB12345678901234567890AB")).isFalse();
    }
}
