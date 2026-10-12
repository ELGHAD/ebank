package com.example.ebank.web;

import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.config.AuthCookies;
import com.example.ebank.dto.ChangePasswordRequest;
import com.example.ebank.dto.LoginRequest;
import com.example.ebank.dto.LoginResponse;
import com.example.ebank.dto.UserResponse;
import com.example.ebank.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookies cookies;

    /**
     * Makes sure the XSRF-TOKEN cookie is set. The SPA calls it once at startup, before any POST.
     * Reading the token is what makes Spring Security write the cookie.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request, HttpServletRequest http) {
        var session = authService.login(request, http.getRemoteAddr());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.session(session.token()).toString())
                .body(session.response());
    }

    /** The authenticated user's profile; used by the SPA to restore a session. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return authService.currentUser(principal);
    }

    @PostMapping("/password")
    public ResponseEntity<LoginResponse> changePassword(@AuthenticationPrincipal AppUserPrincipal principal,
                                                        @RequestBody @Valid ChangePasswordRequest request,
                                                        HttpServletRequest http) {
        var session = authService.changePassword(principal, request, http.getRemoteAddr());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.session(session.token()).toString())
                .body(session.response());
    }

    /**
     * Revokes every session of the caller (all devices) and clears the cookie. Always succeeds,
     * so a client with an already expired session can still clean up its cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal != null) {
            authService.logout(principal);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.cleared().toString())
                .build();
    }
}
