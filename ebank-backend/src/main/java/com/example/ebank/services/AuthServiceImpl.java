package com.example.ebank.services;

import com.example.ebank.audit.AuditEventType;
import com.example.ebank.audit.AuditService;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.config.JwtService;
import com.example.ebank.config.LoginThrottle;
import com.example.ebank.dto.ChangePasswordRequest;
import com.example.ebank.dto.LoginRequest;
import com.example.ebank.dto.LoginResponse;
import com.example.ebank.dto.Mappers;
import com.example.ebank.dto.UserResponse;
import com.example.ebank.entities.AppUser;
import com.example.ebank.exception.BadRequestException;
import com.example.ebank.exception.NotFoundException;
import com.example.ebank.exception.TooManyAttemptsException;
import com.example.ebank.repositories.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** BCrypt only uses the first 72 bytes of a password. */
    private static final int BCRYPT_MAX_BYTES = 72;

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginThrottle throttle;
    private final AuditService audit;

    /**
     * Not transactional on purpose: the audit events of a failed login must be committed even though the
     * login throws, so they are written in their own transactions.
     */
    @Override
    public Session login(LoginRequest request, String clientIp) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);

        Duration wait = throttle.retryAfter(username, clientIp);
        if (!wait.isZero()) {
            audit.recordIndependently(null, null, username, AuditEventType.LOGIN_THROTTLED, null);
            throw new TooManyAttemptsException(wait);
        }

        try {
            // BadCredentialsException / DisabledException, mapped to 401 by GlobalExceptionHandler
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.password()));
        } catch (AuthenticationException e) {
            throttle.recordFailure(username, clientIp);
            AppUser known = userRepo.findByUsername(username).orElse(null);
            audit.recordIndependently(organizationIdOf(known), known == null ? null : known.getId(), username,
                    AuditEventType.LOGIN_FAILED, known == null ? "unknown user" : e.getClass().getSimpleName());
            throw e;
        }
        throttle.recordSuccess(username);

        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user disappeared"));
        audit.recordIndependently(organizationIdOf(user), user.getId(), username, AuditEventType.LOGIN_SUCCEEDED, null);
        return session(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse currentUser(AppUserPrincipal principal) {
        return userRepo.findById(principal.userId())
                .map(Mappers::toUserResponse)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    @Override
    @Transactional
    public Session changePassword(AppUserPrincipal principal, ChangePasswordRequest req, String clientIp) {
        // The current password is a credential check too: it is throttled like a login
        Duration wait = throttle.retryAfter(principal.username(), clientIp);
        if (!wait.isZero()) {
            throw new TooManyAttemptsException(wait);
        }

        AppUser user = userRepo.findById(principal.userId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throttle.recordFailure(principal.username(), clientIp);
            audit.recordIndependently(principal.organizationId(), principal.userId(), principal.username(),
                    AuditEventType.PASSWORD_CHANGE_FAILED, "wrong current password");
            // 400, not 401: the session itself is valid and must not be treated as expired by the client
            throw new BadRequestException("WRONG_CURRENT_PASSWORD", "The current password is incorrect");
        }
        validateNewPassword(req.newPassword(), req.currentPassword(), user.getUsername());

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setMustChangePassword(false);
        user.setTokenVersion(user.getTokenVersion() + 1); // revokes sessions on every other device
        userRepo.saveAndFlush(user);
        throttle.recordSuccess(principal.username());
        audit.record(principal, AuditEventType.PASSWORD_CHANGED, "user:" + user.getId(), null);
        return session(user);
    }

    @Override
    @Transactional
    public void logout(AppUserPrincipal principal) {
        userRepo.findById(principal.userId()).ifPresent(user -> {
            user.setTokenVersion(user.getTokenVersion() + 1);
            audit.record(principal, AuditEventType.LOGOUT, "user:" + user.getId(), null);
        });
    }

    private Session session(AppUser user) {
        return new Session(jwtService.generateToken(user),
                new LoginResponse(jwtService.getExpiration().toSeconds(), Mappers.toUserResponse(user)));
    }

    private static void validateNewPassword(String newPassword, String currentPassword, String username) {
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new BadRequestException("PASSWORD_TOO_LONG", "New password is too long");
        }
        if (newPassword.equals(currentPassword)) {
            throw new BadRequestException("PASSWORD_REUSED", "New password must differ from the current one");
        }
        if (newPassword.toLowerCase(Locale.ROOT).contains(username.split("@")[0].toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("PASSWORD_CONTAINS_USERNAME", "New password must not contain your username");
        }
    }

    private static Long organizationIdOf(AppUser user) {
        return user == null || user.getOrganization() == null ? null : user.getOrganization().getId();
    }
}
