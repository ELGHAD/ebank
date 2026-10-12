package com.example.ebank.services;

import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.ChangePasswordRequest;
import com.example.ebank.dto.LoginRequest;
import com.example.ebank.dto.LoginResponse;
import com.example.ebank.dto.UserResponse;

public interface AuthService {

    /** A new session: the token goes into the cookie, the response into the body. */
    record Session(String token, LoginResponse response) {
    }

    Session login(LoginRequest request, String clientIp);

    UserResponse currentUser(AppUserPrincipal principal);

    /** Changes the password, revokes every existing session and returns a fresh one for the caller. */
    Session changePassword(AppUserPrincipal principal, ChangePasswordRequest request, String clientIp);

    /** Revokes every session of the user (all devices). */
    void logout(AppUserPrincipal principal);
}
