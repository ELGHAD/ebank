package com.example.ebank;

import com.example.ebank.entities.AppRole;
import com.example.ebank.entities.AppUser;
import com.example.ebank.entities.Organization;
import com.example.ebank.repositories.AppRoleRepository;
import com.example.ebank.repositories.AppUserRepository;
import com.example.ebank.repositories.OrganizationRepository;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for HTTP-level integration tests against PostgreSQL.
 * Tests do not clean the database; they isolate themselves with unique names instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    protected static final String AGENT_PASSWORD = "agent-password-123";

    @Autowired protected MockMvc mvc;
    @Autowired protected OrganizationRepository organizationRepo;
    @Autowired protected AppUserRepository userRepo;
    @Autowired protected AppRoleRepository roleRepo;
    @Autowired protected PasswordEncoder passwordEncoder;

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected Organization newOrganization() {
        Organization org = new Organization();
        org.setName(unique("Org"));
        return organizationRepo.save(org);
    }

    /** Creates an agent in the given organization and returns its username. */
    protected String newAgent(Organization org) {
        AppUser agent = new AppUser();
        agent.setOrganization(org);
        agent.setUsername(unique("agent") + "@test.ebank");
        agent.setPasswordHash(passwordEncoder.encode(AGENT_PASSWORD));
        agent.getRoles().add(roleRepo.findByRoleName(AppRole.AGENT).orElseThrow());
        userRepo.save(agent);
        return agent.getUsername();
    }

    protected ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "%s", "password": "%s"}""".formatted(username, password)));
    }

    protected String token(String username, String password) throws Exception {
        String body = login(username, password).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    protected String agentToken(Organization org) throws Exception {
        return token(newAgent(org), AGENT_PASSWORD);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    /** A client created through the API, with its one-time password. */
    protected record OnboardedClient(long id, String username, String temporaryPassword) {
    }

    protected OnboardedClient onboardClient(String agentToken) throws Exception {
        String email = unique("client") + "@test.ebank";
        String body = mvc.perform(post("/api/admin/clients")
                        .header("Authorization", bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identityRef": "%s", "firstName": "Test", "lastName": "Client", "email": "%s"}"""
                                .formatted(unique("ID"), email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(body, "$.client.id");
        return new OnboardedClient(id.longValue(), JsonPath.read(body, "$.username"),
                JsonPath.read(body, "$.temporaryPassword"));
    }

    protected ResultActions openAccount(String agentToken, long clientId, String extraJson) throws Exception {
        return mvc.perform(post("/api/admin/accounts")
                .header("Authorization", bearer(agentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"clientId\": " + clientId + (extraJson.isEmpty() ? "" : ", " + extraJson) + "}"));
    }

    protected ResultActions getWithToken(String url, String token) throws Exception {
        return mvc.perform(get(url).header("Authorization", bearer(token)));
    }
}
