package com.example.ebank.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ClientResponse(Long id, String identityRef, String firstName, String lastName,
                             LocalDate birthDate, String email, String address, Instant createdAt) {
}
