package com.example.ebank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required") String currentPassword,
        @NotBlank(message = "New password is required")
        @Size(min = 12, max = 64, message = "New password must be 12 to 64 characters") String newPassword) {
}
