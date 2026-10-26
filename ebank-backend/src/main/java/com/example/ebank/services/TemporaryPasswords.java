package com.example.ebank.services;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * One-time passwords handed out when an account is created for someone else (client, agent, organization admin).
 * Shown once, stored only as a BCrypt hash, and must be replaced at first sign-in.
 */
public final class TemporaryPasswords {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswords() {
    }

    /** 96 random bits, URL-safe Base64 (16 characters). */
    public static String generate() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
