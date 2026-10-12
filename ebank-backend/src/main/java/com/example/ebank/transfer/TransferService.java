package com.example.ebank.transfer;

import com.example.ebank.banking.MoneyPolicy;
import com.example.ebank.banking.Rib;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.entities.AccountStatus;
import com.example.ebank.entities.BankAccount;
import com.example.ebank.exception.BadRequestException;
import com.example.ebank.exception.BusinessRuleException;
import com.example.ebank.exception.NotFoundException;
import com.example.ebank.repositories.AppUserRepository;
import com.example.ebank.repositories.BankAccountRepository;
import com.example.ebank.transfer.TransferDtos.TransferRequest;
import com.example.ebank.transfer.TransferDtos.TransferResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Executes simulated transfers between accounts of the same organization.
 * <p>
 * Guarantees:
 * <ul>
 *   <li>Atomicity: balances, the transfer row and both ledger entries are written in one transaction.</li>
 *   <li>No overdraft or lost update under concurrency: both account rows are locked with
 *       {@code SELECT ... FOR UPDATE}, always in ascending id order so two opposite transfers cannot deadlock.
 *       Business rules are evaluated only on the locked, current rows.</li>
 *   <li>Idempotency: one transfer per (user, Idempotency-Key), enforced by a unique constraint. A retry with
 *       the same key and payload returns the original transfer; a different payload is rejected.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9_-]{8,100}");

    private final BankAccountRepository accountRepo;
    private final TransferRepository transferRepo;
    private final LedgerEntryRepository ledgerRepo;
    private final AppUserRepository userRepo;
    private final MoneyPolicy moneyPolicy;

    /** @param replayed true when the result is the original transfer of an idempotent retry */
    public record TransferResult(TransferResponse transfer, boolean replayed) {
    }

    @Transactional
    public TransferResult transfer(AppUserPrincipal caller, String idempotencyKey, TransferRequest req) {
        if (idempotencyKey == null || !IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
            throw new BadRequestException("INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key header must be 8 to 100 characters: letters, digits, '-' or '_'");
        }
        String destinationRib = req.destinationRib().trim().toUpperCase();
        String requestHash = hash(req.sourceAccountId(), destinationRib, req.amount(), req.description());

        // 1. Fast path for retries: nothing is locked yet
        Optional<TransferResult> replay = findReplay(caller, idempotencyKey, requestHash);
        if (replay.isPresent()) {
            return replay.get();
        }

        // 2. Cheap validations. They must not load BankAccount entities: an entity already in the persistence
        //    context would be returned as-is (stale) by the locking query below, defeating the lock.
        if (!Rib.isValid(destinationRib)) {
            throw new BadRequestException("INVALID_RIB", "The recipient RIB is not valid (check for typos)");
        }
        if (!accountRepo.existsByIdAndClientAppUserId(req.sourceAccountId(), caller.userId())) {
            // Also the answer for someone else's account: its existence is not revealed
            throw new NotFoundException("SOURCE_ACCOUNT_NOT_FOUND", "Source account not found");
        }
        Long destinationId = accountRepo.findIdByRibAndOrganizationId(destinationRib, caller.organizationId())
                .orElseThrow(() -> new NotFoundException("RECIPIENT_NOT_FOUND", "No account with this RIB"));
        if (destinationId.equals(req.sourceAccountId())) {
            throw new BadRequestException("SAME_ACCOUNT", "Source and recipient accounts must be different");
        }

        // 3. Lock both rows in a global order (ascending id) to rule out deadlocks between opposite transfers
        BankAccount first = lock(Math.min(req.sourceAccountId(), destinationId));
        BankAccount second = lock(Math.max(req.sourceAccountId(), destinationId));
        BankAccount source = first.getId().equals(req.sourceAccountId()) ? first : second;
        BankAccount destination = source == first ? second : first;

        // 4. A concurrent retry with the same key may have committed while we waited for the lock
        replay = findReplay(caller, idempotencyKey, requestHash);
        if (replay.isPresent()) {
            return replay.get();
        }

        // 5. Business rules, on current data
        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("SOURCE_ACCOUNT_NOT_ACTIVE", "The source account is " + source.getStatus());
        }
        if (destination.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("RECIPIENT_ACCOUNT_NOT_ACTIVE", "The recipient account cannot receive transfers");
        }
        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new BusinessRuleException("CURRENCY_MISMATCH",
                    "Transfers are only possible between accounts of the same currency");
        }
        BigDecimal amount = moneyPolicy.requirePositiveAmount(req.amount(), source.getCurrency());
        if (source.getBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("INSUFFICIENT_FUNDS", "Insufficient funds");
        }

        // 6. Apply: balances, transfer, balanced ledger entries — committed together or not at all
        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));
        accountRepo.flush();

        String description = req.description() == null || req.description().isBlank() ? null : req.description().trim();
        Transfer transfer = new Transfer();
        transfer.setOrganization(source.getOrganization());
        transfer.setInitiatedBy(userRepo.getReferenceById(caller.userId()));
        transfer.setIdempotencyKey(idempotencyKey);
        transfer.setRequestHash(requestHash);
        transfer.setSourceAccount(source);
        transfer.setDestinationAccount(destination);
        transfer.setAmount(amount);
        transfer.setCurrency(source.getCurrency());
        transfer.setDescription(description);
        transferRepo.save(transfer);

        ledgerRepo.save(LedgerEntry.of(source, transfer, LedgerEntryType.DEBIT, amount, description));
        ledgerRepo.save(LedgerEntry.of(destination, transfer, LedgerEntryType.CREDIT, amount, description));
        ledgerRepo.flush();

        log.info("Transfer {} completed: account {} -> account {}, {} {}",
                transfer.getId(), source.getId(), destination.getId(), amount, transfer.getCurrency());
        return new TransferResult(toResponse(transfer, source.getBalance()), false);
    }

    private Optional<TransferResult> findReplay(AppUserPrincipal caller, String key, String requestHash) {
        return transferRepo.findByInitiatedByIdAndIdempotencyKey(caller.userId(), key)
                .map(existing -> {
                    if (!existing.getRequestHash().equals(requestHash)) {
                        throw new BusinessRuleException("IDEMPOTENCY_KEY_REUSED",
                                "This Idempotency-Key was already used for a different transfer");
                    }
                    BigDecimal balanceAfter = ledgerRepo
                            .findByTransferIdAndEntryType(existing.getId(), LedgerEntryType.DEBIT)
                            .orElseThrow()
                            .getBalanceAfter();
                    return new TransferResult(toResponse(existing, balanceAfter), true);
                });
    }

    private BankAccount lock(Long accountId) {
        return accountRepo.findByIdForUpdate(accountId).orElseThrow();
    }

    private static TransferResponse toResponse(Transfer t, BigDecimal balanceAfter) {
        return new TransferResponse(t.getId(), t.getSourceAccount().getId(), t.getSourceAccount().getRib(),
                t.getDestinationAccount().getRib(), t.getAmount(), t.getCurrency(), t.getDescription(),
                balanceAfter, t.getCreatedAt());
    }

    /** Canonical form of the request; amounts compare by value (10.5 == 10.50). */
    static String hash(Long sourceAccountId, String destinationRib, BigDecimal amount, String description) {
        String canonical = sourceAccountId + "|" + destinationRib + "|"
                + (amount == null ? "" : amount.stripTrailingZeros().toPlainString()) + "|"
                + (description == null ? "" : description.trim());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
