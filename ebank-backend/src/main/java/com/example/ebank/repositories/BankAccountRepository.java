package com.example.ebank.repositories;

import com.example.ebank.entities.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    // Agent view, scoped to the agent's organization
    List<BankAccount> findByClientIdAndOrganizationIdOrderByIdAsc(Long clientId, Long organizationId);

    // Client view: accounts owned by the authenticated user
    List<BankAccount> findByClientAppUserIdOrderByIdAsc(Long appUserId);

    boolean existsByRib(String rib);
}
