package com.example.ebank.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AppRole {
    public static final String AGENT = "AGENT";
    public static final String CLIENT = "CLIENT";
    public static final String ORG_ADMIN = "ORG_ADMIN";
    public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String roleName;
}
