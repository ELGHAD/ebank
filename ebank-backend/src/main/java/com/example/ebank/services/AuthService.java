package com.example.ebank.services;

import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.LoginRequest;
import com.example.ebank.dto.LoginResponse;
import com.example.ebank.dto.UserResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    UserResponse currentUser(AppUserPrincipal principal);
}
