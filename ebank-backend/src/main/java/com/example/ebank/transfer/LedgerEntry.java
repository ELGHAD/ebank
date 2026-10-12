package com.example.ebank.transfer;

import com.example.ebank.entities.BankAccount;
import com.example.ebank.entities.Organization;
import com.example.ebank.entities.Timestamps;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One movement on one account. The ledger is the source of truth; {@code BankAccount.balance} is a
 * cache kept in the same transaction. Entries are append-only.
 */
@Entity
@Immutable
@Getter @Setter
@NoArgsConstructor
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BankAccount account;

    /** Null for deposits only. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Transfer transfer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LedgerEntryType entryType;

    /** Always positive; the type gives the direction. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Account balance right after this entry (running balance for statements). */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(length = 140)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Timestamps.now();
    }

    static LedgerEntry of(BankAccount account, Transfer transfer, LedgerEntryType type,
                          BigDecimal amount, String description) {
        LedgerEntry entry = new LedgerEntry();
        entry.setOrganization(account.getOrganization());
        entry.setAccount(account);
        entry.setTransfer(transfer);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setBalanceAfter(account.getBalance());
        entry.setDescription(description);
        return entry;
    }
}
