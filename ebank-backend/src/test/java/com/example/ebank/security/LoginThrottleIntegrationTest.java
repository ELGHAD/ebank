package com.example.ebank.security;

import com.example.ebank.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LoginThrottleIntegrationTest extends IntegrationTestSupport {

    @Test
    void fiveFailuresLockTheUsernameEvenForTheRightPassword() throws Exception {
        String agent = newAgent(newOrganization());
        for (int i = 0; i < 5; i++) {
            login(agent, "wrong-password").andExpect(status().isUnauthorized());
        }

        login(agent, AGENT_PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", matchesPattern("\\d+")))
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));

        // Another account is not affected
        login(newAgent(newOrganization()), AGENT_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void successResetsTheFailureCount() throws Exception {
        String agent = newAgent(newOrganization());
        for (int i = 0; i < 4; i++) {
            login(agent, "wrong-password").andExpect(status().isUnauthorized());
        }
        login(agent, AGENT_PASSWORD).andExpect(status().isOk());
        for (int i = 0; i < 4; i++) {
            login(agent, "wrong-password").andExpect(status().isUnauthorized());
        }
        login(agent, AGENT_PASSWORD).andExpect(status().isOk());
    }
}
