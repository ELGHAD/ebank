package com.example.ebank.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AccountOperation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime operationDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    private double amount;

    private String description;

    @ManyToOne
    private BankAccount bankAccount;
}
