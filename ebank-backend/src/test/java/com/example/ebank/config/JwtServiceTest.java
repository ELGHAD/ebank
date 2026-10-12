package com.example.ebank.config;

import com.example.ebank.entities.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private static AppUser user(String username) {
        AppUser user = new AppUser();
        user.setUsername(username);
        return user;
    }

    // Regression for audit S-5: the documented placeholder secret crashed startup with a library error
    @Test
    void secretShorterThan256BitsFailsFastWithActionableMessage() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("ChangeMeJwtSecret", Duration.ofHours(1), "ebank")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes")
                .hasMessageContaining("openssl rand -hex 32");
    }

    @Test
    void tokenRoundTrip() {
        JwtService service = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1), "ebank"));
        String token = service.generateToken(user("alice@test.ebank"));
        assertThat(service.validate(token)).contains(new JwtService.TokenClaims("alice@test.ebank", 0));
    }

    @Test
    void tokenCarriesTheUsersTokenVersion() {
        JwtService service = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1), "ebank"));
        AppUser user = user("alice@test.ebank");
        user.setTokenVersion(7);
        assertThat(service.validate(service.generateToken(user)).orElseThrow().version()).isEqualTo(7);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = new JwtService(new JwtProperties(SECRET, Duration.ofSeconds(-1), "ebank"));
        String token = service.generateToken(user("alice@test.ebank"));
        assertThat(service.validate(token)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyOrIssuerIsRejected() {
        JwtService ours = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1), "ebank"));
        JwtService otherKey = new JwtService(new JwtProperties(SECRET.replace('0', 'x'), Duration.ofHours(1), "ebank"));
        JwtService otherIssuer = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1), "someone-else"));

        assertThat(ours.validate(otherKey.generateToken(user("a@test.ebank")))).isEmpty();
        assertThat(ours.validate(otherIssuer.generateToken(user("a@test.ebank")))).isEmpty();
        assertThat(ours.validate("garbage")).isEmpty();
    }
}
