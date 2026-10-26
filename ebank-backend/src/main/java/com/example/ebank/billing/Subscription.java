package com.example.ebank.billing;

import com.example.ebank.entities.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** One per organization, created by a database trigger when the organization is inserted (see V5). */
@Entity
@Table(name = "subscription")
@Getter @Setter
@NoArgsConstructor
public class Subscription {

    public enum Status {
        ACTIVE,
        /** Simulated failed payment: growth (new clients, accounts, staff) is blocked; money stays accessible. */
        PAST_DUE
    }

    @Id
    private Long organizationId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_code")
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private Instant currentPeriodStart;

    @Column(nullable = false)
    private Instant currentPeriodEnd;

    @Column(nullable = false)
    private Instant updatedAt;

    @PreUpdate
    void onUpdate() {
        updatedAt = Timestamps.now();
    }
}
