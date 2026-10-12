package com.example.ebank.security;

import com.example.ebank.IntegrationTestSupport;
import com.example.ebank.config.AuthCookies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Clients onboarded with a one-time password must choose their own before banking. */
class PasswordChangeIntegrationTest extends IntegrationTestSupport {

    OnboardedClient client;
    String temporaryToken;

    @BeforeEach
    void setUp() throws Exception {
        client = onboardClient(agentToken(newOrganization()));
        temporaryToken = token(client.username(), client.temporaryPassword());
    }

    private ResultActions changePassword(String token, String current, String next) throws Exception {
        return mvc.perform(post("/api/auth/password")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword": "%s", "newPassword": "%s"}""".formatted(current, next)));
    }

    @Test
    void bankingIsBlockedUntilThePasswordIsChanged() throws Exception {
        getWithToken("/api/auth/me", temporaryToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordChangeRequired").value(true));
        getWithToken("/api/client/accounts", temporaryToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
    }

    @Test
    void changingThePasswordUnlocksTheAccountAndRevokesOldSessions() throws Exception {
        String newToken = changePassword(temporaryToken, client.temporaryPassword(), "my-own-strong-pass-7")
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly(AuthCookies.SESSION_COOKIE, true))
                .andExpect(jsonPath("$.user.passwordChangeRequired").value(false))
                .andReturn().getResponse().getCookie(AuthCookies.SESSION_COOKIE).getValue();

        getWithToken("/api/client/accounts", newToken).andExpect(status().isOk());
        getWithToken("/api/auth/me", temporaryToken).andExpect(status().isUnauthorized());
        login(client.username(), client.temporaryPassword()).andExpect(status().isUnauthorized());
        login(client.username(), "my-own-strong-pass-7").andExpect(status().isOk())
                .andExpect(jsonPath("$.user.passwordChangeRequired").value(false));
    }

    @Test
    void wrongCurrentPasswordIs400NotASessionError() throws Exception {
        changePassword(temporaryToken, "not-the-current-one", "my-own-strong-pass-7")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_CURRENT_PASSWORD"));
        getWithToken("/api/auth/me", temporaryToken).andExpect(status().isOk());
    }

    @Test
    void weakOrReusedPasswordsAreRejected() throws Exception {
        changePassword(temporaryToken, client.temporaryPassword(), "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());
        changePassword(temporaryToken, client.temporaryPassword(), client.temporaryPassword())
                .andExpect(status().isBadRequest());
        String localPart = client.username().split("@")[0];
        changePassword(temporaryToken, client.temporaryPassword(), localPart + "-is-my-password")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_CONTAINS_USERNAME"));
    }
}
