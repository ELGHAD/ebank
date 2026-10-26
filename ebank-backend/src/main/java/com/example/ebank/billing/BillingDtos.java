package com.example.ebank.billing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Payloads of the plans and (simulated) billing API. */
public final class BillingDtos {

    private BillingDtos() {
    }

    /** Null limits mean unlimited. */
    public record PlanResponse(String code, String name, BigDecimal monthlyPrice, String currency,
                               Integer maxStaff, Integer maxClients, Integer maxAccounts, Integer maxTransfersPerMonth) {
        static PlanResponse of(Plan p) {
            return new PlanResponse(p.getCode(), p.getName(), p.getMonthlyPrice(), p.getCurrency(), p.getMaxStaff(),
                    p.getMaxClients(), p.getMaxAccounts(), p.getMaxTransfersPerMonth());
        }
    }

    public record UsageItem(String quota, String label, long used, Integer limit) {
    }

    /** Invoices are simulated: issued as paid, no payment provider involved. */
    public record InvoiceResponse(Long id, String planCode, Instant periodStart, Instant periodEnd,
                                  BigDecimal amount, String currency, Instant createdAt) {
        static InvoiceResponse of(Invoice i) {
            return new InvoiceResponse(i.getId(), i.getPlanCode(), i.getPeriodStart(), i.getPeriodEnd(), i.getAmount(),
                    i.getCurrency(), i.getCreatedAt());
        }
    }

    public record SubscriptionResponse(PlanResponse plan, Subscription.Status status, Instant currentPeriodStart,
                                       Instant currentPeriodEnd, List<UsageItem> usage, List<InvoiceResponse> invoices) {
    }

    public record ChangePlanRequest(@NotBlank(message = "Plan code is required") String planCode) {
    }

    public record ChangeSubscriptionStatusRequest(@NotNull(message = "Status is required") Subscription.Status status) {
    }
}
