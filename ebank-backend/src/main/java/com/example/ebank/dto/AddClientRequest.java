package com.example.ebank.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AddClientRequest {
    private String identityRef;
    private String firstname;
    private String lastname;
    private LocalDate birthDate;
    private String email;
    private String address;
}
