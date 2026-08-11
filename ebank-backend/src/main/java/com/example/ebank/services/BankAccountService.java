package com.example.ebank.services;

import com.example.ebank.dto.AddBankAccountRequest;
import com.example.ebank.dto.AddBankAccountResponse;
import com.example.ebank.entities.BankAccount;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface BankAccountService {

    AddBankAccountResponse createAccount(AddBankAccountRequest req);

    // admin / get accounts for a specific client
    List<BankAccount> getAccountsByClient(Long clientId);

    // client / get my own accounts
    List<BankAccount> getMyAccounts(Authentication authentication);
}
