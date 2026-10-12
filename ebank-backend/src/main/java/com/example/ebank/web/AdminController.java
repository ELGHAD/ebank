package com.example.ebank.web;

import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.AccountResponse;
import com.example.ebank.dto.ChangeAccountStatusRequest;
import com.example.ebank.dto.ClientDetailsResponse;
import com.example.ebank.dto.ClientSummaryResponse;
import com.example.ebank.dto.PageResponse;
import com.example.ebank.dto.OnboardClientRequest;
import com.example.ebank.dto.OnboardClientResponse;
import com.example.ebank.dto.OpenAccountRequest;
import com.example.ebank.services.BankAccountService;
import com.example.ebank.services.ClientService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent operations. Every operation is scoped to the agent's organization.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENT')")
public class AdminController {

    private final ClientService clientService;
    private final BankAccountService bankAccountService;

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public OnboardClientResponse onboardClient(@AuthenticationPrincipal AppUserPrincipal agent,
                                               @RequestBody @Valid OnboardClientRequest request) {
        return clientService.onboardClient(agent, request);
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse openAccount(@AuthenticationPrincipal AppUserPrincipal agent,
                                       @RequestBody @Valid OpenAccountRequest request) {
        return bankAccountService.openAccount(agent, request);
    }

    @GetMapping("/clients")
    public PageResponse<ClientSummaryResponse> searchClients(@AuthenticationPrincipal AppUserPrincipal agent,
                                                             @RequestParam(required = false) @Size(max = 100) String q,
                                                             @RequestParam(defaultValue = "0") @Min(0) int page,
                                                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return clientService.searchClients(agent, q, page, size);
    }

    @GetMapping("/clients/{clientId}")
    public ClientDetailsResponse getClient(@AuthenticationPrincipal AppUserPrincipal agent, @PathVariable Long clientId) {
        return clientService.getClient(agent, clientId);
    }

    @GetMapping("/accounts/{accountId}")
    public AccountResponse getAccount(@AuthenticationPrincipal AppUserPrincipal agent, @PathVariable Long accountId) {
        return bankAccountService.getAccount(agent, accountId);
    }

    @PatchMapping("/accounts/{accountId}/status")
    public AccountResponse changeAccountStatus(@AuthenticationPrincipal AppUserPrincipal agent,
                                               @PathVariable Long accountId,
                                               @RequestBody @Valid ChangeAccountStatusRequest request) {
        return bankAccountService.changeStatus(agent, accountId, request.status());
    }

    @GetMapping("/clients/{clientId}/accounts")
    public List<AccountResponse> getAccountsOfClient(@AuthenticationPrincipal AppUserPrincipal agent,
                                                     @PathVariable Long clientId) {
        return bankAccountService.getAccountsOfClient(agent, clientId);
    }
}
