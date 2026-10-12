package com.example.ebank.dto;

import com.example.ebank.entities.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(Long id, String rib, String currency, BigDecimal balance,
                              AccountStatus status, Long clientId, Instant createdAt) {
}
