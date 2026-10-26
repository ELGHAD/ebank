package com.example.ebank.organization;

import com.example.ebank.entities.OrganizationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Payloads of the platform (organization lifecycle) and organization-admin (staff, overview) APIs. */
public final class OrganizationDtos {

    private OrganizationDtos() {
    }

    // ---------- Platform administration ----------

    public record CreateOrganizationRequest(
            @NotBlank(message = "Organization name is required")
            @Size(min = 2, max = 120, message = "Organization name must be 2 to 120 characters") String name,
            @NotBlank(message = "Administrator email is required")
            @Email(message = "Administrator email must be a valid address")
            @Size(max = 254) String adminEmail) {
    }

    public record OrganizationSummary(Long id, String name, OrganizationStatus status, Instant createdAt,
                                      long users, long clients, long accounts, String plan, String subscriptionStatus) {
    }

    /** The first administrator's one-time password is shown once to the platform admin. */
    public record CreateOrganizationResponse(OrganizationSummary organization, String adminUsername,
                                             String temporaryPassword) {
    }

    public record ChangeOrganizationStatusRequest(@NotNull(message = "Status is required") OrganizationStatus status) {
    }

    // ---------- Organization administration ----------

    public record CurrencyAmount(String currency, BigDecimal amount) {
    }

    public record OrganizationOverview(String organizationName, long clients, long accounts, long activeAccounts,
                                       long blockedAccounts, long closedAccounts, List<CurrencyAmount> balances,
                                       long transfersLast30Days, List<CurrencyAmount> transferVolumeLast30Days,
                                       long staff) {
    }

    public record StaffMember(Long id, String username, List<String> roles, boolean enabled,
                              boolean passwordChangeRequired, Instant createdAt) {
    }

    public record CreateStaffRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be a valid address")
            @Size(max = 254) String email,
            @NotNull(message = "Role is required")
            @Pattern(regexp = "AGENT|ORG_ADMIN", message = "Role must be AGENT or ORG_ADMIN") String role) {
    }

    public record CreateStaffResponse(StaffMember member, String temporaryPassword) {
    }

    public record ChangeStaffEnabledRequest(@NotNull(message = "enabled is required") Boolean enabled) {
    }
}
