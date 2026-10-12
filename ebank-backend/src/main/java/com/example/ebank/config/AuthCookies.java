package com.example.ebank.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * The session cookie carrying the JWT. HttpOnly: page scripts (and therefore an XSS payload) cannot read it.
 * SameSite=Strict + CSRF tokens: other sites cannot make the browser use it.
 */
@Component
@RequiredArgsConstructor
public class AuthCookies {

    public static final String SESSION_COOKIE = "EBANK_SESSION";
    private static final String PATH = "/api";

    private final CookieProperties properties;
    private final JwtService jwtService;

    public ResponseCookie session(String token) {
        return base(token).maxAge(jwtService.getExpiration()).build();
    }

    public ResponseCookie cleared() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(SESSION_COOKIE, value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite("Strict")
                .path(PATH);
    }
}
