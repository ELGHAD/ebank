package com.example.ebank.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A tenant of the platform (a simulated bank). Every tenant-owned row references one.
 */
@Entity
@Getter @Setter
@NoArgsConstructor
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isActive() {
        return status == OrganizationStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        createdAt = Timestamps.now();
    }
}
