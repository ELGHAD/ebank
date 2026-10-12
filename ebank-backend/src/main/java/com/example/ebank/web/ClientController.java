package com.example.ebank.web;

import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.AccountResponse;
import com.example.ebank.services.BankAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientController {

    private final BankAccountService bankAccountService;

    @GetMapping("/accounts")
    public List<AccountResponse> myAccounts(@AuthenticationPrincipal AppUserPrincipal principal) {
        return bankAccountService.getMyAccounts(principal);
    }
}
