package com.example.ebank.services;

import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.AccountResponse;
import com.example.ebank.entities.AccountStatus;
import com.example.ebank.dto.OpenAccountRequest;

import java.util.List;

public interface BankAccountService {

    AccountResponse openAccount(AppUserPrincipal agent, OpenAccountRequest request);

    // agent: accounts of a client of the agent's organization
    List<AccountResponse> getAccountsOfClient(AppUserPrincipal agent, Long clientId);

    AccountResponse getAccount(AppUserPrincipal agent, Long accountId);

    /** ACTIVE <-> BLOCKED; CLOSED is final and requires a zero balance. */
    AccountResponse changeStatus(AppUserPrincipal agent, Long accountId, AccountStatus status);

    // client: the caller's own accounts
    List<AccountResponse> getMyAccounts(AppUserPrincipal client);
}
