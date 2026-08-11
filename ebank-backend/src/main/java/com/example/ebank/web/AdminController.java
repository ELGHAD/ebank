package com.example.ebank.web;

import com.example.ebank.dto.AddBankAccountRequest;
import com.example.ebank.dto.AddBankAccountResponse;
import com.example.ebank.dto.AddClientRequest;
import com.example.ebank.dto.AddClientResponse;
import com.example.ebank.entities.BankAccount;
import com.example.ebank.services.BankAccountService;
import com.example.ebank.services.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENT_GUICHET')")
public class AdminController {

    private final ClientService clientService;
    private final BankAccountService bankAccountService;

    /**
     * UC-2 : Ajouter un client
     */
    @PostMapping("/clients")
    public AddClientResponse addClient(@RequestBody @Valid AddClientRequest request) {
        return clientService.addClient(request);
    }

    /**
     * UC-3 : Créer un compte bancaire pour un client
     */
    @PostMapping("/accounts")
    public AddBankAccountResponse createAccount(@RequestBody @Valid AddBankAccountRequest request) {
        return bankAccountService.createAccount(request);
    }

    /**
     * UC-3 (consultation côté admin) :
     * Récupérer les comptes d'un client à partir de son ID
     */
    @GetMapping("/clients/{clientId}/accounts")
    public List<BankAccount> getAccountsForClient(@PathVariable Long clientId) {
        return bankAccountService.getAccountsByClient(clientId);
    }
}
