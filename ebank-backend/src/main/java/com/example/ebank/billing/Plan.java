package com.example.ebank.billing;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/** Subscription plan (reference data). A null limit means unlimited. Read-only for the application (see V5). */
@Entity
@Table(name = "plan")
@Immutable
@Getter
@NoArgsConstructor
public class Plan {
    @Id
    private String code;
    private String name;
    private BigDecimal monthlyPrice;
    private String currency;
    private Integer maxStaff;
    private Integer maxClients;
    private Integer maxAccounts;
    private Integer maxTransfersPerMonth;
    private int sortOrder;

    public Integer limitOf(Quota quota) {
        return switch (quota) {
            case STAFF -> maxStaff;
            case CLIENTS -> maxClients;
            case ACCOUNTS -> maxAccounts;
            case TRANSFERS_PER_MONTH -> maxTransfersPerMonth;
        };
    }
}
