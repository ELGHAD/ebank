package com.example.ebank.repositories;

import com.example.ebank.entities.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    // For admin / by client id (needs BankAccount.client.id)
    List<BankAccount> findByClient_Id(Long clientId);

    // For "my accounts" by authenticated username
    // (needs BankAccount.client.appUser.username)
    List<BankAccount> findByClient_AppUser_Username(String username);
}
