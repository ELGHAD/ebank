package com.example.ebank.config;

/**
 * The authenticated caller, built from the database on every request (not from token claims),
 * so role changes and account disabling take effect immediately.
 */
public record AppUserPrincipal(Long userId, String username, Long organizationId) {
}
