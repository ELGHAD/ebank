package com.example.ebank.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter @Setter
@NoArgsConstructor
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Client client;

    /** Server-generated synthetic account identifier, see {@code com.example.ebank.banking.Rib}. */
    @Column(unique = true, nullable = false, length = 34)
    private String rib;

    /** ISO 4217 code. */
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Timestamps.now();
    }
}
