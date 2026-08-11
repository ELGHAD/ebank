package com.example.ebank.services;

import com.example.ebank.config.JwtService;
import com.example.ebank.dto.LoginRequest;
import com.example.ebank.dto.LoginResponse;
import com.example.ebank.entities.AppUser;
import com.example.ebank.repositories.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepo;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {

        var authToken = new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
        );
        authenticationManager.authenticate(authToken);

        AppUser appUser = userRepo.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(appUser);
        String role = appUser.getRoles().stream()
                .findFirst()
                .map(r -> r.getRoleName())
                .orElse("CLIENT");

        long expiresInMs = 3600000L;

        return new LoginResponse(token, appUser.getUsername(), role, expiresInMs / 1000);
    }
}
