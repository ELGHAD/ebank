package com.example.ebank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record OnboardClientRequest(
        @NotBlank(message = "Identity reference is required")
        @Size(max = 50) String identityRef,
        @NotBlank(message = "First name is required")
        @Size(max = 100) String firstName,
        @NotBlank(message = "Last name is required")
        @Size(max = 100) String lastName,
        @Past(message = "Birth date must be in the past") LocalDate birthDate,
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 254) String email,
        @Size(max = 255) String address) {
}
