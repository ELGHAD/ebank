package com.example.ebank.audit;

import com.example.ebank.entities.Timestamps;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/** Append-only record of a security-relevant or business event. Never contains secrets. */
@Entity
@Immutable
@Getter @Setter
@NoArgsConstructor
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long organizationId;

    private Long actorUserId;

    @Column(length = 254)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditEventType eventType;

    /** What the event is about, e.g. "account:12" or "transfer:7". */
    @Column(length = 100)
    private String target;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 255)
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Timestamps.now();
    }
}
