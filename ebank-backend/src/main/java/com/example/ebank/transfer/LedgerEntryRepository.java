package com.example.ebank.transfer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long>, JpaSpecificationExecutor<LedgerEntry> {

    // History pages show the counterparty RIB: fetch the transfer and both accounts in the same query (no N+1)
    @Override
    @EntityGraph(attributePaths = {"transfer", "transfer.sourceAccount", "transfer.destinationAccount"})
    Page<LedgerEntry> findAll(Specification<LedgerEntry> spec, Pageable pageable);

    Optional<LedgerEntry> findByTransferIdAndEntryType(Long transferId, LedgerEntryType entryType);

    /**
     * Accounts whose stored balance differs from what their ledger entries add up to. Must always be empty.
     */
    @Query(value = """
            select a.id
            from bank_account a
            left join (select account_id,
                              sum(case when entry_type = 'DEBIT' then -amount else amount end) as total
                       from ledger_entry
                       group by account_id) l on l.account_id = a.id
            where a.balance <> coalesce(l.total, 0)
            order by a.id
            """, nativeQuery = true)
    List<Long> findUnreconciledAccountIds();
}
