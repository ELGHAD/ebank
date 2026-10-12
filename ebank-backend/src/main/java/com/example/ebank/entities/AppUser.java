package com.example.ebank.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null only for future platform-level administrators. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Organization organization;

    /** The user's email address, lower-cased. */
    @Column(unique = true, nullable = false, length = 254)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    /** Must be changed before any banking operation (set for clients onboarded with a one-time password). */
    @Column(nullable = false)
    private boolean mustChangePassword = false;

    /** Embedded in issued tokens; incrementing it revokes every token issued before (logout, password change). */
    @Column(nullable = false)
    private int tokenVersion = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<AppRole> roles = new HashSet<>();

    @PrePersist
    void onCreate() {
        createdAt = Timestamps.now();
    }
}
