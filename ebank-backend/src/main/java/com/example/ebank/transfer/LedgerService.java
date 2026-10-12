package com.example.ebank.transfer;

import com.example.ebank.entities.BankAccount;
import com.example.ebank.transfer.TransferDtos.LedgerEntryResponse;
import com.example.ebank.transfer.TransferDtos.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerService {

    public static final int MAX_PAGE_SIZE = 100;

    private final LedgerEntryRepository ledgerRepo;

    /**
     * Credits simulated funds to an account. Must run inside the caller's transaction, after the
     * account balance has been increased, so the entry and the balance commit together.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordDeposit(BankAccount account, BigDecimal amount, String description) {
        ledgerRepo.save(LedgerEntry.of(account, null, LedgerEntryType.DEPOSIT, amount, description));
    }

    /**
     * Account history, newest first. Authorization (ownership / tenant) is checked by the caller.
     *
     * @param from inclusive start date (UTC), may be null
     * @param to   inclusive end date (UTC), may be null
     */
    @Transactional(readOnly = true)
    public PageResponse<LedgerEntryResponse> history(Long accountId, LedgerEntryType type,
                                                     LocalDate from, LocalDate to, int page, int size) {
        Specification<LedgerEntry> spec = (root, query, cb) -> cb.equal(root.get("account").get("id"), accountId);
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entryType"), type));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay().toInstant(ZoneOffset.UTC)));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
        }

        Page<LedgerEntry> result = ledgerRepo.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return new PageResponse<>(
                result.getContent().stream().map(LedgerService::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /** Ids of accounts whose cached balance disagrees with the ledger. Empty when the books are consistent. */
    @Transactional(readOnly = true)
    public List<Long> findUnreconciledAccountIds() {
        return ledgerRepo.findUnreconciledAccountIds();
    }

    private static LedgerEntryResponse toResponse(LedgerEntry e) {
        String counterparty = null;
        if (e.getTransfer() != null) {
            counterparty = e.getEntryType() == LedgerEntryType.DEBIT
                    ? e.getTransfer().getDestinationAccount().getRib()
                    : e.getTransfer().getSourceAccount().getRib();
        }
        return new LedgerEntryResponse(e.getId(), e.getEntryType(), e.getAmount(), e.getBalanceAfter(),
                e.getDescription(), e.getTransfer() == null ? null : e.getTransfer().getId(),
                counterparty, e.getCreatedAt());
    }
}
