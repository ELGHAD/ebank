package com.example.ebank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddClientResponse {
    private Long id;
    private String identityRef;
    private String email;
    private String message;
}
