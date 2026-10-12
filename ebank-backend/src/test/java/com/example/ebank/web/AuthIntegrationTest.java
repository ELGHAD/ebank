package com.example.ebank.web;

import com.example.ebank.IntegrationTestSupport;
import com.example.ebank.entities.AppUser;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Test
    void loginReturnsTokenAndUserWithoutSecrets() throws Exception {
        String agent = newAgent(newOrganization());

        login(agent, AGENT_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900)) // 15m from application-test.yml
                .andExpect(jsonPath("$.user.username").value(agent))
                .andExpect(jsonPath("$.user.roles", contains("AGENT")))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void loginIsCaseInsensitiveOnUsername() throws Exception {
        String agent = newAgent(newOrganization());
        login(agent.toUpperCase(), AGENT_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSame401() throws Exception {
        String agent = newAgent(newOrganization());

        login(agent, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        login("nobody@test.ebank", "whatever")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void blankCredentialsAreAValidationError() throws Exception {
        login("", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    // Regression for audit S-4: disabled users could log in and keep using their tokens
    @Test
    void disabledUserCannotLoginAndExistingTokenStopsWorking() throws Exception {
        String agent = newAgent(newOrganization());
        String token = token(agent, AGENT_PASSWORD);

        AppUser user = userRepo.findByUsername(agent).orElseThrow();
        user.setEnabled(false);
        userRepo.save(user);

        login(agent, AGENT_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        getWithToken("/api/auth/me", token).andExpect(status().isUnauthorized());
    }

    // Regression for audit S-6: missing or invalid tokens returned 403 with an empty body
    @Test
    void missingOrInvalidTokenIs401WithProblemBody() throws Exception {
        mvc.perform(get("/api/client/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        getWithToken("/api/client/accounts", "not.a.jwt")
                .andExpect(status().isUnauthorized());
    }

    // Regression: /api/auth/me was public and returned a fixed string
    @Test
    void meRequiresAuthenticationAndReturnsTheCaller() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        String agent = newAgent(newOrganization());
        getWithToken("/api/auth/me", token(agent, AGENT_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(agent))
                .andExpect(jsonPath("$.roles", contains("AGENT")));
    }

    @Test
    void wrongRoleIs403() throws Exception {
        String agentToken = agentToken(newOrganization());
        getWithToken("/api/client/accounts", agentToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // Regression for audit S-7: in a real container every error (even 404) became 403
    @Test
    void unknownRouteIs404ForAuthenticatedCaller() throws Exception {
        getWithToken("/api/admin/does-not-exist", agentToken(newOrganization()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
