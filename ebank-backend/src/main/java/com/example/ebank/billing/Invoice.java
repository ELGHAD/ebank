package com.example.ebank.billing;

import com.example.ebank.entities.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Simulated invoice: no payment provider is involved and no card data exists anywhere.
 * Append-only (trigger + no UPDATE/DELETE grant); one per organization and billing period.
 */
@Entity
@Table(name = "invoice")
@Immutable
@Getter @Setter
@NoArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long organizationId;
    private String planCode;
    private Instant periodStart;
    private Instant periodEnd;
    private BigDecimal amount;
    private String currency;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Timestamps.now();
    }
}
