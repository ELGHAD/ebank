package com.example.ebank.dto;

import lombok.Data;

@Data
public class AddBankAccountResponse {
    private Long accountId;
    private String rib;
    private String message;
}
