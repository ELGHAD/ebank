package com.example.ebank.banking;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.random.RandomGenerator;

/**
 * Synthetic account identifier for the simulation (not a real RIB/IBAN).
 * <p>
 * Format: {@code EB} + 2 check digits + 20 random digits (24 characters), e.g. {@code EB4712345678901234567890}.
 * The check digits use the ISO 7064 MOD 97-10 scheme from IBAN, so a mistyped digit in a recipient
 * identifier is detected before any lookup.
 */
public final class Rib {

    public static final String PREFIX = "EB";
    public static final int LENGTH = 24;
    private static final int BODY_DIGITS = 20;
    private static final BigInteger NINETY_SEVEN = BigInteger.valueOf(97);
    private static final RandomGenerator RANDOM = new SecureRandom();

    private Rib() {
    }

    public static String generate() {
        return generate(RANDOM);
    }

    static String generate(RandomGenerator random) {
        StringBuilder body = new StringBuilder(BODY_DIGITS);
        for (int i = 0; i < BODY_DIGITS; i++) {
            body.append(random.nextInt(10));
        }
        return PREFIX + checkDigits(body.toString()) + body;
    }

    public static boolean isValid(String rib) {
        if (rib == null || rib.length() != LENGTH || !rib.startsWith(PREFIX)) {
            return false;
        }
        for (int i = PREFIX.length(); i < LENGTH; i++) {
            if (!Character.isDigit(rib.charAt(i))) {
                return false;
            }
        }
        String rearranged = rib.substring(4) + numeric(PREFIX) + rib.substring(2, 4);
        return new BigInteger(rearranged).mod(NINETY_SEVEN).intValue() == 1;
    }

    private static String checkDigits(String body) {
        int remainder = new BigInteger(body + numeric(PREFIX) + "00").mod(NINETY_SEVEN).intValue();
        return String.format("%02d", 98 - remainder);
    }

    /** Letters to numbers as in IBAN: A=10 ... Z=35. */
    private static String numeric(String letters) {
        StringBuilder sb = new StringBuilder();
        for (char c : letters.toCharArray()) {
            sb.append(c - 'A' + 10);
        }
        return sb.toString();
    }
}
