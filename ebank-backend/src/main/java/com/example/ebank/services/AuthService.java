package com.example.ebank.services;

import com.example.ebank.dto.LoginRequest;
import com.example.ebank.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
