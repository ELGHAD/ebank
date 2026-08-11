package com.example.ebank.web;

import com.example.ebank.entities.BankAccount;
import com.example.ebank.services.BankAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientController {

    private final BankAccountService bankAccountService;

    @GetMapping("/accounts")
    public List<BankAccount> myAccounts(Authentication authentication) {
        return bankAccountService.getMyAccounts(authentication);
    }
}
