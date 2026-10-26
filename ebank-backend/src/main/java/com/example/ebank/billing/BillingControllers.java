package com.example.ebank.billing;

import com.example.ebank.billing.BillingDtos.ChangePlanRequest;
import com.example.ebank.billing.BillingDtos.ChangeSubscriptionStatusRequest;
import com.example.ebank.billing.BillingDtos.PlanResponse;
import com.example.ebank.billing.BillingDtos.SubscriptionResponse;
import com.example.ebank.config.AppUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** HTTP endpoints and the renewal scheduler of the billing module. */
public final class BillingControllers {

    private BillingControllers() {
    }

    @RestController
    @RequestMapping("/api/org")
    @RequiredArgsConstructor
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public static class OrgBillingController {

        private final BillingService billingService;

        @GetMapping("/plans")
        public List<PlanResponse> plans() {
            return billingService.plans();
        }

        @GetMapping("/subscription")
        public SubscriptionResponse subscription(@AuthenticationPrincipal AppUserPrincipal admin) {
            return billingService.subscription(admin.organizationId());
        }

        @PutMapping("/subscription/plan")
        public SubscriptionResponse changePlan(@AuthenticationPrincipal AppUserPrincipal admin,
                                               @RequestBody @Valid ChangePlanRequest request) {
            return billingService.changePlan(admin, request.planCode());
        }
    }

    /** Simulates the payment provider's notifications. A real integration would be a signed webhook instead. */
    @RestController
    @RequestMapping("/api/platform/organizations")
    @RequiredArgsConstructor
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public static class PlatformBillingController {

        private final BillingService billingService;

        @PatchMapping("/{organizationId}/subscription-status")
        public Map<String, Object> changeStatus(@AuthenticationPrincipal AppUserPrincipal admin,
                                                @PathVariable Long organizationId,
                                                @RequestBody @Valid ChangeSubscriptionStatusRequest request) {
            return Map.of("organizationId", organizationId,
                    "status", billingService.changeStatus(admin, organizationId, request.status()));
        }
    }

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "app.billing.scheduler-enabled", havingValue = "true", matchIfMissing = true)
    static class SchedulingConfig {
    }

    @Component
    @RequiredArgsConstructor
    @ConditionalOnProperty(name = "app.billing.scheduler-enabled", havingValue = "true", matchIfMissing = true)
    static class RenewalJob {

        private final BillingService billingService;

        @Scheduled(fixedDelayString = "${app.billing.renewal-interval:1h}", initialDelayString = "1m")
        void renew() {
            billingService.renewDueSubscriptions();
        }
    }
}
