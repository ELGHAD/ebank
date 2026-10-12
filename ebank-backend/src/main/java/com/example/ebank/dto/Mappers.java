package com.example.ebank.dto;

import com.example.ebank.entities.AppRole;
import com.example.ebank.entities.AppUser;
import com.example.ebank.entities.BankAccount;
import com.example.ebank.entities.Client;

/** Entity to DTO conversions. Must be called inside a transaction when lazy associations are read. */
public final class Mappers {

    private Mappers() {
    }

    public static UserResponse toUserResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRoles().stream().map(AppRole::getRoleName).sorted().toList(),
                user.getOrganization() == null ? null : user.getOrganization().getId(),
                user.isMustChangePassword());
    }

    public static ClientResponse toClientResponse(Client client) {
        return new ClientResponse(client.getId(), client.getIdentityRef(), client.getFirstName(),
                client.getLastName(), client.getBirthDate(), client.getAppUser().getUsername(),
                client.getAddress(), client.getCreatedAt());
    }

    public static AccountResponse toAccountResponse(BankAccount account) {
        return new AccountResponse(account.getId(), account.getRib(), account.getCurrency(),
                account.getBalance(), account.getStatus(), account.getClient().getId(), account.getCreatedAt());
    }
}
