package com.example.ebank.repositories;

import com.example.ebank.entities.BankAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    // Agent view, scoped to the agent's organization
    List<BankAccount> findByClientIdAndOrganizationIdOrderByIdAsc(Long clientId, Long organizationId);

    // Client view: accounts owned by the authenticated user
    List<BankAccount> findByClientAppUserIdOrderByIdAsc(Long appUserId);

    boolean existsByRib(String rib);

    /** Rows: [organizationId, count]. */
    @Query("select a.organization.id, count(a) from BankAccount a where a.organization.id in :ids group by a.organization.id")
    List<Object[]> countByOrganizationIds(@Param("ids") Collection<Long> ids);

    /** Rows: [status, count]. */
    @Query("select a.status, count(a) from BankAccount a where a.organization.id = :organizationId group by a.status")
    List<Object[]> countByStatus(@Param("organizationId") Long organizationId);

    /** Rows: [currency, total balance]. */
    @Query("select a.currency, sum(a.balance) from BankAccount a where a.organization.id = :organizationId "
            + "group by a.currency order by a.currency")
    List<Object[]> sumBalancesByCurrency(@Param("organizationId") Long organizationId);

    // Ownership / tenancy checks that do not load the entity (see TransferService for why that matters)
    boolean existsByIdAndClientAppUserId(Long id, Long appUserId);

    boolean existsByIdAndOrganizationId(Long id, Long organizationId);

    Optional<BankAccount> findByIdAndOrganizationId(Long id, Long organizationId);

    /** Number of accounts per client, for a page of clients, in one query. Rows: [clientId, count]. */
    @Query("select a.client.id, count(a) from BankAccount a where a.client.id in :clientIds group by a.client.id")
    List<Object[]> countByClientIds(@Param("clientIds") List<Long> clientIds);

    @Query("select a.id from BankAccount a where a.rib = :rib and a.organization.id = :organizationId")
    Optional<Long> findIdByRibAndOrganizationId(@Param("rib") String rib, @Param("organizationId") Long organizationId);

    /** {@code SELECT ... FOR UPDATE}: blocks concurrent writers of this row until the transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BankAccount a where a.id = :id")
    Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);
}
