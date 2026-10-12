package com.example.ebank.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Organization organization;

    /** Login identity; its username is the client's email address. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private AppUser appUser;

    /** National ID / passport reference, unique within the organization. */
    @Column(nullable = false, length = 50)
    private String identityRef;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    private LocalDate birthDate;

    private String address;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
