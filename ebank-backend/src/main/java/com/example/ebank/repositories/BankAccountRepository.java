package com.example.ebank.repositories;

import com.example.ebank.entities.BankAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    // Agent view, scoped to the agent's organization
    List<BankAccount> findByClientIdAndOrganizationIdOrderByIdAsc(Long clientId, Long organizationId);

    // Client view: accounts owned by the authenticated user
    List<BankAccount> findByClientAppUserIdOrderByIdAsc(Long appUserId);

    boolean existsByRib(String rib);

    // Ownership / tenancy checks that do not load the entity (see TransferService for why that matters)
    boolean existsByIdAndClientAppUserId(Long id, Long appUserId);

    boolean existsByIdAndOrganizationId(Long id, Long organizationId);

    @Query("select a.id from BankAccount a where a.rib = :rib and a.organization.id = :organizationId")
    Optional<Long> findIdByRibAndOrganizationId(@Param("rib") String rib, @Param("organizationId") Long organizationId);

    /** {@code SELECT ... FOR UPDATE}: blocks concurrent writers of this row until the transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BankAccount a where a.id = :id")
    Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);
}
