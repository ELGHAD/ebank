package com.example.ebank.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String identityRef;

    private String firstname;
    private String lastname;
    private LocalDate birthDate;

    @Column(unique = true, nullable = false)
    private String email;

    private String address;

    @OneToOne
    private AppUser appUser;
}
