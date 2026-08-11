package com.example.ebank.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String rib;

    private double balance;

    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.OUVERT;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JsonIgnoreProperties({"bankAccounts"}) // ou {"accounts"} selon ton mapping
    private Client client;


    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL)
    private List<AccountOperation> operations = new ArrayList<>();
}
