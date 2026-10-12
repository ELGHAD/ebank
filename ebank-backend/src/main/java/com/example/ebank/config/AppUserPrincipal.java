package com.example.ebank.config;

/**
 * The authenticated caller, built from the database on every request (not from token claims),
 * so role changes, account disabling and password-change requirements take effect immediately.
 */
public record AppUserPrincipal(Long userId, String username, Long organizationId, boolean passwordChangeRequired) {

    public AppUserPrincipal(Long userId, String username, Long organizationId) {
        this(userId, username, organizationId, false);
    }
}
