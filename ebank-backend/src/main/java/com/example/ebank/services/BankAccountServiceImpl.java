package com.example.ebank.services;

import com.example.ebank.dto.AddBankAccountRequest;
import com.example.ebank.dto.AddBankAccountResponse;
import com.example.ebank.entities.AccountStatus;
import com.example.ebank.entities.BankAccount;
import com.example.ebank.entities.Client;
import com.example.ebank.repositories.BankAccountRepository;
import com.example.ebank.repositories.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final ClientRepository clientRepository;

    @Override
    public AddBankAccountResponse createAccount(AddBankAccountRequest req) {
        Client client = clientRepository.findById(req.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        BankAccount acc = new BankAccount();
        acc.setClient(client);
        acc.setRib(req.getRib());
        acc.setBalance(req.getBalance());
        acc.setStatus(AccountStatus.OUVERT);
        acc.setCreatedAt(LocalDateTime.now());

        bankAccountRepository.save(acc);

        AddBankAccountResponse res = new AddBankAccountResponse();
        res.setAccountId(acc.getId());
        res.setRib(acc.getRib());
        res.setMessage("Bank account created successfully");
        return res;
    }

    @Override
    public List<BankAccount> getAccountsByClient(Long clientId) {
        // IMPORTANT: use findByClient_Id NOT findByClientId
        return bankAccountRepository.findByClient_Id(clientId);
    }

    @Override
    public List<BankAccount> getMyAccounts(Authentication authentication) {
        String username = authentication.getName();
        return bankAccountRepository.findByClient_AppUser_Username(username);
    }
}
