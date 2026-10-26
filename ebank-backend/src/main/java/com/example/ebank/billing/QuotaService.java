package com.example.ebank.billing;

import com.example.ebank.entities.AccountStatus;
import com.example.ebank.exception.BusinessRuleException;
import com.example.ebank.exception.QuotaExceededException;
import com.example.ebank.repositories.AppUserRepository;
import com.example.ebank.repositories.BankAccountRepository;
import com.example.ebank.repositories.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The single place where plan limits are enforced. Both entry points run inside the caller's transaction:
 * if the business operation fails afterwards, the consumed slot is rolled back with it.
 */
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final SubscriptionRepository subscriptionRepo;
    private final AppUserRepository userRepo;
    private final ClientRepository clientRepo;
    private final BankAccountRepository accountRepo;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    /**
     * Before creating a client, account or staff member (or re-enabling one). Locks the organization's subscription
     * row so that two concurrent requests cannot both take the last slot; blocked while the subscription is PAST_DUE.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireGrowth(Long organizationId, Quota quota) {
        Subscription subscription = subscriptionRepo.findByIdForUpdate(organizationId).orElseThrow();
        if (subscription.getStatus() == Subscription.Status.PAST_DUE) {
            throw new BusinessRuleException("SUBSCRIPTION_PAST_DUE",
                    "Your bank's subscription payment is overdue: new clients, accounts and staff are blocked until it is settled. "
                            + "Existing customers keep full access to their money.");
        }
        Plan plan = subscription.getPlan();
        Integer limit = plan.limitOf(quota);
        if (limit != null && usage(organizationId, quota) >= limit) {
            throw new QuotaExceededException(quota.name(), quota.label(), limit, plan.getName());
        }
    }

    /**
     * Counts one transfer against the monthly limit with a single atomic statement: the counter is incremented only
     * while it is below the limit, so concurrent transfers cannot exceed it, and only the counter row is locked
     * (transfers of different accounts do not queue behind a whole-organization lock).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void consumeTransfer(Long organizationId) {
        Plan plan = subscriptionRepo.findById(organizationId).orElseThrow().getPlan();
        Integer limit = plan.getMaxTransfersPerMonth();
        List<Integer> updated = jdbc.queryForList("""
                insert into usage_counter (organization_id, period_month, transfers) values (?, ?, 1)
                on conflict (organization_id, period_month)
                do update set transfers = usage_counter.transfers + 1
                where ?::integer is null or usage_counter.transfers < ?::integer
                returning transfers""", Integer.class, organizationId, currentMonth(), limit, limit);
        if (updated.isEmpty()) {
            throw new QuotaExceededException(Quota.TRANSFERS_PER_MONTH.name(), Quota.TRANSFERS_PER_MONTH.label(),
                    limit, plan.getName());
        }
    }

    @Transactional(readOnly = true)
    public Map<Quota, Long> usage(Long organizationId) {
        Map<Quota, Long> usage = new EnumMap<>(Quota.class);
        for (Quota quota : Quota.values()) {
            usage.put(quota, usage(organizationId, quota));
        }
        return usage;
    }

    long usage(Long organizationId, Quota quota) {
        return switch (quota) {
            case STAFF -> userRepo.countEnabledStaff(organizationId);
            case CLIENTS -> clientRepo.countByOrganizationId(organizationId);
            case ACCOUNTS -> accountRepo.countByOrganizationIdAndStatusNot(organizationId, AccountStatus.CLOSED);
            case TRANSFERS_PER_MONTH -> jdbc.queryForList(
                    "select transfers from usage_counter where organization_id = ? and period_month = ?",
                    Long.class, organizationId, currentMonth()).stream().findFirst().orElse(0L);
        };
    }

    /** Usage periods are calendar months in UTC. */
    private LocalDate currentMonth() {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC)).withDayOfMonth(1);
    }
}
