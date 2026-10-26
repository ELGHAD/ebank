package com.example.ebank.billing;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /** Serializes quota-consuming operations of one organization: two requests cannot both take the last slot. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Subscription s where s.organizationId = :organizationId")
    Optional<Subscription> findByIdForUpdate(@Param("organizationId") Long organizationId);

    @Query("select s.organizationId from Subscription s where s.currentPeriodEnd <= :now")
    List<Long> findDueForRenewal(@Param("now") Instant now);

    List<Subscription> findByOrganizationIdIn(Collection<Long> organizationIds);
}
