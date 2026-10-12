package com.example.ebank.services;

import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.ClientDetailsResponse;
import com.example.ebank.dto.ClientSummaryResponse;
import com.example.ebank.dto.OnboardClientRequest;
import com.example.ebank.dto.PageResponse;
import com.example.ebank.dto.OnboardClientResponse;

public interface ClientService {
    OnboardClientResponse onboardClient(AppUserPrincipal agent, OnboardClientRequest request);

    /** Clients of the agent's organization, optionally filtered by name, email or identity reference. */
    PageResponse<ClientSummaryResponse> searchClients(AppUserPrincipal agent, String query, int page, int size);

    ClientDetailsResponse getClient(AppUserPrincipal agent, Long clientId);
}
