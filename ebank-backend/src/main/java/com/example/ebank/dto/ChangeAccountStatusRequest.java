package com.example.ebank.dto;

import com.example.ebank.entities.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeAccountStatusRequest(@NotNull(message = "Status is required") AccountStatus status) {
}
