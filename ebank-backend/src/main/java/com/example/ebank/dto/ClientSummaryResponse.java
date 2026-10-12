package com.example.ebank.dto;

import java.time.Instant;

/** One row of the agent's client list. */
public record ClientSummaryResponse(Long id, String identityRef, String firstName, String lastName, String email,
                                    long accountCount, Instant createdAt) {
}
