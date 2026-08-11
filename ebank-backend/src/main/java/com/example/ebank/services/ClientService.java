package com.example.ebank.services;

import com.example.ebank.dto.AddClientRequest;
import com.example.ebank.dto.AddClientResponse;

public interface ClientService {
    AddClientResponse addClient(AddClientRequest request);
}
