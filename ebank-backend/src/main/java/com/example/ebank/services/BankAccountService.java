package com.example.ebank.services;

import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.AccountResponse;
import com.example.ebank.dto.OpenAccountRequest;

import java.util.List;

public interface BankAccountService {

    AccountResponse openAccount(AppUserPrincipal agent, OpenAccountRequest request);

    // agent: accounts of a client of the agent's organization
    List<AccountResponse> getAccountsOfClient(AppUserPrincipal agent, Long clientId);

    // client: the caller's own accounts
    List<AccountResponse> getMyAccounts(AppUserPrincipal client);
}
