package com.example.ebank.organization;

import com.example.ebank.IntegrationTestSupport;
import com.example.ebank.entities.AppRole;
import com.example.ebank.entities.Organization;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Organization lifecycle by platform administrators. */
class PlatformIntegrationTest extends IntegrationTestSupport {

    String platformToken;

    @BeforeEach
    void setUp() throws Exception {
        platformToken = staffToken(null, AppRole.PLATFORM_ADMIN);
    }

    private ResultActions createOrganization(String name, String adminEmail) throws Exception {
        return mvc.perform(post("/api/platform/organizations")
                .header("Authorization", bearer(platformToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "adminEmail": "%s"}""".formatted(name, adminEmail)));
    }

    private ResultActions setStatus(long orgId, String status) throws Exception {
        return mvc.perform(patch("/api/platform/organizations/" + orgId + "/status")
                .header("Authorization", bearer(platformToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"" + status + "\"}"));
    }

    @Test
    void provisionsAnOrganizationWithItsFirstAdministrator() throws Exception {
        String name = unique("Atlas Bank");
        String adminEmail = unique("founder") + "@atlas.test";
        String body = createOrganization(name, adminEmail)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organization.name").value(name))
                .andExpect(jsonPath("$.organization.status").value("ACTIVE"))
                .andExpect(jsonPath("$.adminUsername").value(adminEmail))
                .andReturn().getResponse().getContentAsString();
        String temporaryPassword = JsonPath.read(body, "$.temporaryPassword");

        // The new administrator must replace the one-time password, then administers only that organization
        String temporaryToken = token(adminEmail, temporaryPassword);
        getWithToken("/api/org/overview", temporaryToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
        login(adminEmail, temporaryPassword).andExpect(jsonPath("$.user.roles", contains("ORG_ADMIN")));
    }

    @Test
    void creationIsAtomic() throws Exception {
        String takenEmail = newAgent(newOrganization());
        String name = unique("Half Bank");

        createOrganization(name, takenEmail)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
        assertThat(organizationRepo.findByName(name)).as("no half-created organization").isEmpty();
    }

    @Test
    void organizationNamesAreUniqueIgnoringCase() throws Exception {
        String name = unique("Unique Bank");
        createOrganization(name, unique("a") + "@u.test").andExpect(status().isCreated());
        createOrganization(name.toUpperCase(), unique("b") + "@u.test")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_NAME_TAKEN"));
    }

    @Test
    void invalidInputIsRejected() throws Exception {
        createOrganization("", "not-an-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.adminEmail").exists());
    }

    @Test
    void listShowsCountsButNoTenantData() throws Exception {
        Organization org = newOrganization();
        String agentToken = agentToken(org);
        OnboardedClient client = onboardClient(agentToken);
        newAccount(agentToken, client.id(), "100");

        String body = getWithToken("/api/platform/organizations?size=100", platformToken)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String row = "$.content[?(@.id == " + org.getId() + ")].";
        assertThat(JsonPath.<List<Integer>>read(body, row + "clients")).containsExactly(1);
        assertThat(JsonPath.<List<Integer>>read(body, row + "accounts")).containsExactly(1);
        assertThat(JsonPath.<List<Integer>>read(body, row + "users")).containsExactly(2);
        assertThat(body).doesNotContain(client.username()).doesNotContain("balance");
    }

    // The platform administrator operates the platform but never sees a tenant's customers or money
    @Test
    void platformAdministratorCannotReachTenantData() throws Exception {
        getWithToken("/api/admin/clients", platformToken).andExpect(status().isForbidden());
        getWithToken("/api/admin/audit-events", platformToken).andExpect(status().isForbidden());
        getWithToken("/api/org/overview", platformToken).andExpect(status().isForbidden());
        getWithToken("/api/client/accounts", platformToken).andExpect(status().isForbidden());
    }

    @Test
    void onlyPlatformAdministratorsReachThePlatformApi() throws Exception {
        Organization org = newOrganization();
        getWithToken("/api/platform/organizations", agentToken(org)).andExpect(status().isForbidden());
        getWithToken("/api/platform/organizations", staffToken(org, AppRole.ORG_ADMIN)).andExpect(status().isForbidden());
    }

    @Test
    void suspendingAnOrganizationCutsOffAllItsUsersImmediately() throws Exception {
        Organization org = newOrganization();
        String agent = newAgent(org);
        String agentToken = token(agent, AGENT_PASSWORD);
        String otherOrgAgentToken = agentToken(newOrganization());

        setStatus(org.getId(), "SUSPENDED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        getWithToken("/api/admin/clients", agentToken).andExpect(status().isUnauthorized());
        login(agent, AGENT_PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        getWithToken("/api/admin/clients", otherOrgAgentToken).andExpect(status().isOk());

        // Data is kept: reactivation restores access
        setStatus(org.getId(), "ACTIVE").andExpect(jsonPath("$.status").value("ACTIVE"));
        getWithToken("/api/admin/clients", agentToken).andExpect(status().isOk());
    }

    @Test
    void lifecycleEventsAreAuditedInTheOrganizationsTrail() throws Exception {
        String body = createOrganization(unique("Audited Bank"), unique("adm") + "@a.test")
                .andReturn().getResponse().getContentAsString();
        long orgId = ((Number) JsonPath.read(body, "$.organization.id")).longValue();
        setStatus(orgId, "SUSPENDED");
        setStatus(orgId, "ACTIVE");

        Organization org = organizationRepo.findById(orgId).orElseThrow();
        getWithToken("/api/admin/audit-events", staffToken(org, AppRole.ORG_ADMIN))
                .andExpect(jsonPath("$.content[*].type", hasItems(
                        "ORGANIZATION_CREATED", "ORGANIZATION_STATUS_CHANGED")))
                .andExpect(content().string(not(containsString("temporaryPassword"))));
    }

    @Test
    void unknownOrganizationIs404() throws Exception {
        setStatus(999_999_999L, "SUSPENDED").andExpect(status().isNotFound());
    }
}
