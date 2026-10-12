package com.example.ebank.config;

import com.example.ebank.entities.AppUser;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    /** HS256 requires a key of at least 256 bits. */
    static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration expiration;
    private final String issuer;

    public JwtService(JwtProperties properties) {
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            // Fail fast with an actionable message instead of a library stack trace.
            throw new IllegalStateException("app.jwt.secret (env APP_JWT_SECRET) must be at least "
                    + MIN_SECRET_BYTES + " bytes; generate one with: openssl rand -hex 32");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.expiration = properties.expiration();
        this.issuer = properties.issuer();
    }

    public String generateToken(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Returns the username if the token is well-formed, correctly signed, issued by us and not expired. */
    public Optional<String> validateAndGetUsername(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Optional.ofNullable(subject);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Duration getExpiration() {
        return expiration;
    }
}
