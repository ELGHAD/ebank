package com.example.ebank.web;

import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.LoginRequest;
import com.example.ebank.dto.LoginResponse;
import com.example.ebank.dto.UserResponse;
import com.example.ebank.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    /** The authenticated user's profile; used by the SPA to restore a session. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return authService.currentUser(principal);
    }
}
