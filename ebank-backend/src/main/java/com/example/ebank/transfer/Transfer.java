package com.example.ebank.transfer;

import com.example.ebank.entities.AppUser;
import com.example.ebank.entities.BankAccount;
import com.example.ebank.entities.Organization;
import com.example.ebank.entities.Timestamps;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A completed transfer. Rows are only written for successful transfers and are never modified
 * ({@link Immutable} here, an append-only trigger in the database).
 */
@Entity
@Immutable
@Getter @Setter
@NoArgsConstructor
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AppUser initiatedBy;

    @Column(nullable = false, length = 100)
    private String idempotencyKey;

    /** SHA-256 of the request payload, to detect a key reused for a different request. */
    @Column(nullable = false, length = 64)
    private String requestHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BankAccount sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BankAccount destinationAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 140)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Timestamps.now();
    }
}
