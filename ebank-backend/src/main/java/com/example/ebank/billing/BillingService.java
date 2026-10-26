package com.example.ebank.billing;

import com.example.ebank.audit.AuditEventType;
import com.example.ebank.audit.AuditService;
import com.example.ebank.billing.BillingDtos.InvoiceResponse;
import com.example.ebank.billing.BillingDtos.PlanResponse;
import com.example.ebank.billing.BillingDtos.SubscriptionResponse;
import com.example.ebank.billing.BillingDtos.UsageItem;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.exception.BusinessRuleException;
import com.example.ebank.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Subscriptions and SIMULATED billing: plan changes, monthly renewal with invoices, and simulated payment events.
 * No payment provider is called and no payment data is stored.
 */
@Service
@RequiredArgsConstructor
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    /** Renewal catches up at most this many missed periods per run (e.g. after downtime). */
    private static final int MAX_PERIODS_PER_RUN = 24;

    private final PlanRepository planRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final InvoiceRepository invoiceRepo;
    private final QuotaService quotaService;
    private final AuditService audit;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<PlanResponse> plans() {
        return planRepo.findAllByOrderBySortOrderAsc().stream().map(PlanResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse subscription(Long organizationId) {
        Subscription s = subscriptionRepo.findById(organizationId).orElseThrow();
        Map<Quota, Long> usage = quotaService.usage(organizationId);
        List<UsageItem> items = new ArrayList<>();
        for (Quota quota : Quota.values()) {
            items.add(new UsageItem(quota.name(), quota.label(), usage.get(quota), s.getPlan().limitOf(quota)));
        }
        return new SubscriptionResponse(PlanResponse.of(s.getPlan()), s.getStatus(), s.getCurrentPeriodStart(),
                s.getCurrentPeriodEnd(), items,
                invoiceRepo.findTop12ByOrganizationIdOrderByPeriodStartDesc(organizationId).stream()
                        .map(InvoiceResponse::of).toList());
    }

    /**
     * Switches plan immediately and starts a new monthly period (a paid plan issues its simulated invoice).
     * A downgrade is refused while current usage exceeds the target plan's limits.
     */
    @Transactional
    public SubscriptionResponse changePlan(AppUserPrincipal admin, String planCode) {
        Long orgId = admin.organizationId();
        Plan target = planRepo.findById(planCode)
                .orElseThrow(() -> new NotFoundException("PLAN_NOT_FOUND", "Unknown plan: " + planCode));
        Subscription s = subscriptionRepo.findByIdForUpdate(orgId).orElseThrow();
        if (s.getPlan().getCode().equals(target.getCode())) {
            return subscription(orgId);
        }
        boolean toFree = target.getMonthlyPrice().signum() == 0;
        if (s.getStatus() == Subscription.Status.PAST_DUE && !toFree) {
            throw new BusinessRuleException("SUBSCRIPTION_PAST_DUE",
                    "The subscription payment is overdue: settle it before changing to a paid plan");
        }
        List<String> exceeded = new ArrayList<>();
        for (Quota quota : Quota.values()) {
            Integer limit = target.limitOf(quota);
            long used = quotaService.usage(orgId, quota);
            if (limit != null && used > limit) {
                exceeded.add(used + " " + quota.label() + " (limit " + limit + ")");
            }
        }
        if (!exceeded.isEmpty()) {
            throw new BusinessRuleException("PLAN_LIMITS_EXCEEDED",
                    "Current usage is above the " + target.getName() + " plan: " + String.join(", ", exceeded));
        }

        String previous = s.getPlan().getName();
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        s.setPlan(target);
        s.setCurrentPeriodStart(now);
        s.setCurrentPeriodEnd(plusOneMonth(now));
        if (toFree) {
            s.setStatus(Subscription.Status.ACTIVE); // nothing left to pay
        }
        audit.record(admin, AuditEventType.SUBSCRIPTION_PLAN_CHANGED, "organization:" + orgId,
                previous + " -> " + target.getName());
        issueInvoiceIfPaid(s);
        return subscription(orgId);
    }

    /** Simulates the payment provider's notification (failed payment / payment received); platform admins only. */
    @Transactional
    public Subscription.Status changeStatus(AppUserPrincipal platformAdmin, Long organizationId, Subscription.Status status) {
        Subscription s = subscriptionRepo.findByIdForUpdate(organizationId)
                .orElseThrow(() -> new NotFoundException("ORGANIZATION_NOT_FOUND", "Organization not found"));
        if (s.getStatus() != status) {
            audit.recordFor(organizationId, platformAdmin, AuditEventType.SUBSCRIPTION_STATUS_CHANGED,
                    "organization:" + organizationId, s.getStatus() + " -> " + status + " (simulated payment event)");
            s.setStatus(status);
        }
        return s.getStatus();
    }

    /**
     * Advances every subscription whose period has ended, issuing one simulated invoice per new period of a paid
     * plan (none while PAST_DUE). Each organization is processed in its own transaction under a row lock; the
     * unique (organization, period start) constraint makes a concurrent or repeated run harmless.
     *
     * @return number of invoices issued
     */
    public int renewDueSubscriptions() {
        Instant now = Instant.now(clock);
        int issued = 0;
        for (Long orgId : subscriptionRepo.findDueForRenewal(now)) {
            try {
                Integer count = transactionTemplate.execute(tx -> renew(orgId, now));
                issued += count == null ? 0 : count;
            } catch (RuntimeException e) {
                log.error("Renewal failed for organization {}", orgId, e);
            }
        }
        return issued;
    }

    private int renew(Long orgId, Instant now) {
        Subscription s = subscriptionRepo.findByIdForUpdate(orgId).orElseThrow();
        int issued = 0;
        for (int i = 0; i < MAX_PERIODS_PER_RUN && !s.getCurrentPeriodEnd().isAfter(now); i++) {
            s.setCurrentPeriodStart(s.getCurrentPeriodEnd());
            s.setCurrentPeriodEnd(plusOneMonth(s.getCurrentPeriodEnd()));
            if (s.getStatus() == Subscription.Status.ACTIVE && issueInvoiceIfPaid(s)) {
                issued++;
            }
        }
        return issued;
    }

    private boolean issueInvoiceIfPaid(Subscription s) {
        Plan plan = s.getPlan();
        if (plan.getMonthlyPrice().signum() == 0
                || invoiceRepo.existsByOrganizationIdAndPeriodStart(s.getOrganizationId(), s.getCurrentPeriodStart())) {
            return false;
        }
        Invoice invoice = new Invoice();
        invoice.setOrganizationId(s.getOrganizationId());
        invoice.setPlanCode(plan.getCode());
        invoice.setPeriodStart(s.getCurrentPeriodStart());
        invoice.setPeriodEnd(s.getCurrentPeriodEnd());
        invoice.setAmount(plan.getMonthlyPrice());
        invoice.setCurrency(plan.getCurrency());
        invoiceRepo.save(invoice);
        audit.recordSystem(s.getOrganizationId(), AuditEventType.INVOICE_ISSUED, "invoice",
                plan.getName() + " " + plan.getMonthlyPrice().toPlainString() + " " + plan.getCurrency() + " (simulated)");
        return true;
    }

    private static Instant plusOneMonth(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC).plusMonths(1).toInstant();
    }
}
