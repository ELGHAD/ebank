package com.example.ebank.services;

import com.example.ebank.audit.AuditEventType;
import com.example.ebank.audit.AuditService;
import com.example.ebank.banking.MoneyPolicy;
import com.example.ebank.billing.Quota;
import com.example.ebank.billing.QuotaService;
import com.example.ebank.banking.Rib;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.AccountResponse;
import com.example.ebank.dto.Mappers;
import com.example.ebank.dto.OpenAccountRequest;
import com.example.ebank.entities.AccountStatus;
import com.example.ebank.entities.BankAccount;
import com.example.ebank.entities.Client;
import com.example.ebank.exception.BusinessRuleException;
import com.example.ebank.exception.NotFoundException;
import com.example.ebank.repositories.BankAccountRepository;
import com.example.ebank.repositories.ClientRepository;
import com.example.ebank.transfer.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private static final int MAX_RIB_ATTEMPTS = 5;

    private final BankAccountRepository bankAccountRepository;
    private final ClientRepository clientRepository;
    private final MoneyPolicy moneyPolicy;
    private final LedgerService ledgerService;
    private final AuditService audit;
    private final QuotaService quotaService;

    /**
     * The initial deposit is recorded as a DEPOSIT ledger entry in the same transaction,
     * so the new balance is explained by the ledger like every later movement.
     */
    @Override
    @Transactional
    public AccountResponse openAccount(AppUserPrincipal agent, OpenAccountRequest req) {
        Client client = findClientOfOrganization(agent, req.clientId());
        String currency = moneyPolicy.resolveCurrency(req.currency());
        quotaService.requireGrowth(agent.organizationId(), Quota.ACCOUNTS);

        BankAccount account = new BankAccount();
        account.setOrganization(client.getOrganization());
        account.setClient(client);
        account.setRib(newUniqueRib());
        account.setCurrency(currency);
        BigDecimal initialDeposit = moneyPolicy.requireNonNegativeAmount(req.initialDeposit(), currency);
        account.setBalance(initialDeposit);
        account.setStatus(AccountStatus.ACTIVE);
        bankAccountRepository.saveAndFlush(account);
        if (initialDeposit.signum() > 0) {
            ledgerService.recordDeposit(account, initialDeposit, "Opening deposit");
        }
        audit.record(agent, AuditEventType.ACCOUNT_OPENED, "account:" + account.getId(),
                "client " + client.getId() + ", " + currency + " " + initialDeposit.toPlainString());

        return Mappers.toAccountResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsOfClient(AppUserPrincipal agent, Long clientId) {
        findClientOfOrganization(agent, clientId);
        return bankAccountRepository.findByClientIdAndOrganizationIdOrderByIdAsc(clientId, agent.organizationId())
                .stream().map(Mappers::toAccountResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(AppUserPrincipal client) {
        return bankAccountRepository.findByClientAppUserIdOrderByIdAsc(client.userId())
                .stream().map(Mappers::toAccountResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(AppUserPrincipal agent, Long accountId) {
        return bankAccountRepository.findByIdAndOrganizationId(accountId, agent.organizationId())
                .map(Mappers::toAccountResponse)
                .orElseThrow(() -> new NotFoundException("ACCOUNT_NOT_FOUND", "Account not found"));
    }

    @Override
    @Transactional
    public AccountResponse changeStatus(AppUserPrincipal agent, Long accountId, AccountStatus status) {
        if (!bankAccountRepository.existsByIdAndOrganizationId(accountId, agent.organizationId())) {
            throw new NotFoundException("ACCOUNT_NOT_FOUND", "Account not found");
        }
        // Same row lock as transfers: closing cannot race with a credit arriving at the same moment
        BankAccount account = bankAccountRepository.findByIdForUpdate(accountId).orElseThrow();
        AccountStatus previous = account.getStatus();
        if (previous == status) {
            return Mappers.toAccountResponse(account);
        }
        if (previous == AccountStatus.CLOSED) {
            throw new BusinessRuleException("ACCOUNT_CLOSED", "A closed account cannot be reopened");
        }
        if (status == AccountStatus.CLOSED && account.getBalance().signum() != 0) {
            throw new BusinessRuleException("BALANCE_NOT_ZERO",
                    "Only an account with a zero balance can be closed (transfer the remaining funds first)");
        }
        account.setStatus(status);
        audit.record(agent, AuditEventType.ACCOUNT_STATUS_CHANGED, "account:" + account.getId(),
                previous + " -> " + status);
        return Mappers.toAccountResponse(account);
    }

    private Client findClientOfOrganization(AppUserPrincipal agent, Long clientId) {
        return clientRepository.findByIdAndOrganizationId(clientId, agent.organizationId())
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client not found"));
    }

    private String newUniqueRib() {
        // 10^20 possible bodies: a collision is practically impossible; the unique constraint is the real guarantee
        for (int i = 0; i < MAX_RIB_ATTEMPTS; i++) {
            String rib = Rib.generate();
            if (!bankAccountRepository.existsByRib(rib)) {
                return rib;
            }
        }
        throw new IllegalStateException("Could not generate a unique RIB");
    }
}
