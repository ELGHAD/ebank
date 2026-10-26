package com.example.ebank.transfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByInitiatedByIdAndIdempotencyKey(Long userId, String idempotencyKey);

    /** Rows: [currency, count, total amount] of the organization's transfers since the given instant. */
    @Query("select t.currency, count(t), sum(t.amount) from Transfer t where t.organization.id = :organizationId "
            + "and t.createdAt >= :since group by t.currency order by t.currency")
    List<Object[]> statsSince(@Param("organizationId") Long organizationId, @Param("since") Instant since);
}
