package com.example.ebank.web;

import com.example.ebank.IntegrationTestSupport;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.OnboardClientRequest;
import com.example.ebank.entities.Organization;
import com.example.ebank.services.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClientOnboardingIntegrationTest extends IntegrationTestSupport {

    @Autowired
    ClientService clientService;

    // Regression for audit S-2: every client had the same hard-coded password
    @Test
    void onboardedClientsGetDistinctOneTimePasswordsThatWork() throws Exception {
        String agentToken = agentToken(newOrganization());

        OnboardedClient first = onboardClient(agentToken);
        OnboardedClient second = onboardClient(agentToken);

        assertThat(first.temporaryPassword()).hasSizeGreaterThanOrEqualTo(16)
                .isNotEqualTo(second.temporaryPassword());
        login(first.username(), first.temporaryPassword()).andExpect(status().isOk());
        login(first.username(), "client123").andExpect(status().isUnauthorized());
    }

    // Regression for audit D-5: two emails with the same local part collided
    @Test
    void usernameIsTheFullEmailSoSameLocalPartDoesNotCollide() throws Exception {
        String agentToken = agentToken(newOrganization());
        String local = unique("same");

        postClient(agentToken, unique("ID"), local + "@a.test").andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(local + "@a.test"));
        postClient(agentToken, unique("ID"), local + "@b.test").andExpect(status().isCreated());
    }

    @Test
    void duplicateEmailOrIdentityRefIs409() throws Exception {
        String agentToken = agentToken(newOrganization());
        String identityRef = unique("ID");
        String email = unique("dup") + "@test.ebank";
        postClient(agentToken, identityRef, email).andExpect(status().isCreated());

        postClient(agentToken, unique("ID"), email.toUpperCase())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
        postClient(agentToken, identityRef, unique("other") + "@test.ebank")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDENTITY_REF_TAKEN"));
    }

    @Test
    void sameIdentityRefIsAllowedInAnotherOrganization() throws Exception {
        String identityRef = unique("ID");
        postClient(agentToken(newOrganization()), identityRef, unique("a") + "@test.ebank")
                .andExpect(status().isCreated());
        postClient(agentToken(newOrganization()), identityRef, unique("b") + "@test.ebank")
                .andExpect(status().isCreated());
    }

    // Regression for audit S-8: invalid input was accepted or crashed with an NPE
    @Test
    void invalidInputIsRejectedWithFieldErrors() throws Exception {
        mvc.perform(post("/api/admin/clients")
                        .header("Authorization", bearer(agentToken(newOrganization())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "birthDate": "2999-01-01"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("Email must be a valid address"))
                .andExpect(jsonPath("$.errors.identityRef").exists())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.birthDate").exists());
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/api/admin/clients")
                        .header("Authorization", bearer(agentToken(newOrganization())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clientCannotOnboardClients() throws Exception {
        OnboardedClient client = onboardClient(agentToken(newOrganization()));
        postClient(token(client.username(), client.temporaryPassword()), unique("ID"), unique("x") + "@test.ebank")
                .andExpect(status().isForbidden());
    }

    // Regression for audit D-3: a failure after the user insert left an orphan login-capable user
    @Test
    void onboardingIsAtomicWhenTheClientInsertFails() {
        Organization org = newOrganization();
        Long agentId = userRepo.findByUsername(newAgent(org)).orElseThrow().getId();
        String email = unique("atomic") + "@test.ebank";
        // Bypasses controller validation on purpose: last_name is varchar(100), so the second insert fails
        var request = new OnboardClientRequest(unique("ID"), "Test", "x".repeat(101), null, email, null);

        assertThatThrownBy(() -> clientService.onboardClient(new AppUserPrincipal(agentId, "agent", org.getId()), request))
                .isInstanceOf(Exception.class);
        assertThat(userRepo.existsByUsername(email)).as("user row rolled back").isFalse();
    }

    private org.springframework.test.web.servlet.ResultActions postClient(String token, String identityRef, String email)
            throws Exception {
        return mvc.perform(post("/api/admin/clients")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"identityRef": "%s", "firstName": "A", "lastName": "B", "email": "%s"}"""
                        .formatted(identityRef, email)));
    }
}
