package com.example.ebank.transfer;

import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.exception.NotFoundException;
import com.example.ebank.repositories.BankAccountRepository;
import com.example.ebank.transfer.TransferDtos.LedgerEntryResponse;
import com.example.ebank.dto.PageResponse;
import com.example.ebank.transfer.TransferDtos.TransferRequest;
import com.example.ebank.transfer.TransferDtos.TransferResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final LedgerService ledgerService;
    private final BankAccountRepository accountRepo;

    /**
     * Executes a transfer from one of the caller's accounts. Requires an {@code Idempotency-Key} header
     * (a fresh random value per transfer attempt, reused when retrying that same attempt).
     * 201 for a new transfer; 200 with {@code Idempotent-Replayed: true} when replaying a previous result.
     */
    @PostMapping("/api/client/transfers")
    public ResponseEntity<TransferResponse> transfer(@AuthenticationPrincipal AppUserPrincipal caller,
                                                     @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                     @RequestBody @Valid TransferRequest request) {
        var result = transferService.transfer(caller, idempotencyKey, request);
        if (result.replayed()) {
            return ResponseEntity.ok().header("Idempotent-Replayed", "true").body(result.transfer());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.transfer());
    }

    /** History of one of the caller's own accounts. */
    @GetMapping("/api/client/accounts/{accountId}/entries")
    public PageResponse<LedgerEntryResponse> myAccountEntries(
            @AuthenticationPrincipal AppUserPrincipal caller,
            @PathVariable Long accountId,
            @RequestParam(required = false) LedgerEntryType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(LedgerService.MAX_PAGE_SIZE) int size) {
        if (!accountRepo.existsByIdAndClientAppUserId(accountId, caller.userId())) {
            throw new NotFoundException("ACCOUNT_NOT_FOUND", "Account not found");
        }
        return ledgerService.history(accountId, type, from, to, page, size);
    }

    /** History of any account of the agent's organization. */
    @GetMapping("/api/admin/accounts/{accountId}/entries")
    public PageResponse<LedgerEntryResponse> accountEntries(
            @AuthenticationPrincipal AppUserPrincipal agent,
            @PathVariable Long accountId,
            @RequestParam(required = false) LedgerEntryType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(LedgerService.MAX_PAGE_SIZE) int size) {
        if (!accountRepo.existsByIdAndOrganizationId(accountId, agent.organizationId())) {
            throw new NotFoundException("ACCOUNT_NOT_FOUND", "Account not found");
        }
        return ledgerService.history(accountId, type, from, to, page, size);
    }
}
