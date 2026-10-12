package com.example.ebank.services;

import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.config.JwtService;
import com.example.ebank.dto.LoginRequest;
import com.example.ebank.dto.LoginResponse;
import com.example.ebank.dto.Mappers;
import com.example.ebank.dto.UserResponse;
import com.example.ebank.entities.AppUser;
import com.example.ebank.exception.NotFoundException;
import com.example.ebank.repositories.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepo;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        // Throws BadCredentialsException / DisabledException, mapped to 401 by GlobalExceptionHandler
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.password()));

        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user disappeared"));

        return new LoginResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpiration().toSeconds(),
                Mappers.toUserResponse(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse currentUser(AppUserPrincipal principal) {
        return userRepo.findById(principal.userId())
                .map(Mappers::toUserResponse)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }
}
