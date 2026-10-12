package com.example.ebank.services;

import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.OnboardClientRequest;
import com.example.ebank.dto.OnboardClientResponse;

public interface ClientService {
    OnboardClientResponse onboardClient(AppUserPrincipal agent, OnboardClientRequest request);
}
