package com.example.ebank.dto;

import lombok.Data;

@Data
public class AddBankAccountRequest {
    private Long clientId;
    private String rib;
    private double balance;
}
