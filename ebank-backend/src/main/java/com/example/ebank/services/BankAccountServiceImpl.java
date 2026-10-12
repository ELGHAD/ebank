package com.example.ebank.services;

import com.example.ebank.banking.MoneyPolicy;
import com.example.ebank.banking.Rib;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.AccountResponse;
import com.example.ebank.dto.Mappers;
import com.example.ebank.dto.OpenAccountRequest;
import com.example.ebank.entities.AccountStatus;
import com.example.ebank.entities.BankAccount;
import com.example.ebank.entities.Client;
import com.example.ebank.exception.NotFoundException;
import com.example.ebank.repositories.BankAccountRepository;
import com.example.ebank.repositories.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private static final int MAX_RIB_ATTEMPTS = 5;

    private final BankAccountRepository bankAccountRepository;
    private final ClientRepository clientRepository;
    private final MoneyPolicy moneyPolicy;

    /**
     * Phase 1 stores the initial deposit directly in the balance. Phase 2 (ledger) will record it as an
     * explicit opening-deposit entry so that every balance is explained by ledger entries.
     */
    @Override
    @Transactional
    public AccountResponse openAccount(AppUserPrincipal agent, OpenAccountRequest req) {
        Client client = findClientOfOrganization(agent, req.clientId());
        String currency = moneyPolicy.resolveCurrency(req.currency());

        BankAccount account = new BankAccount();
        account.setOrganization(client.getOrganization());
        account.setClient(client);
        account.setRib(newUniqueRib());
        account.setCurrency(currency);
        account.setBalance(moneyPolicy.requireNonNegativeAmount(req.initialDeposit(), currency));
        account.setStatus(AccountStatus.ACTIVE);
        bankAccountRepository.saveAndFlush(account);

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
