package com.example.ebank.dto;

/**
 * Returned once to the agent who onboarded the client. The temporary password is never stored in clear
 * and cannot be retrieved again.
 */
public record OnboardClientResponse(ClientResponse client, String username, String temporaryPassword) {
}
