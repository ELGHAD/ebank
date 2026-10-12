package com.example.ebank;

import com.example.ebank.config.AuthCookies;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    protected static final String CLIENT_PASSWORD = "chosen-by-the-client-42";

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

    /**
     * Real double-submit CSRF, exactly like the SPA: fetch the XSRF-TOKEN cookie, then send it back both as a
     * cookie and in the X-XSRF-TOKEN header. (spring-security-test's csrf() is not used: it swaps the token
     * repository inside the shared filter chain, which leaks into other test classes.)
     */
    protected RequestPostProcessor realCsrf() throws Exception {
        Cookie xsrf = mvc.perform(get("/api/auth/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        return request -> {
            Cookie[] existing = request.getCookies() == null ? new Cookie[0] : request.getCookies();
            Cookie[] cookies = Arrays.copyOf(existing, existing.length + 1);
            cookies[existing.length] = xsrf;
            request.setCookies(cookies);
            request.addHeader("X-XSRF-TOKEN", xsrf.getValue());
            return request;
        };
    }

    /** Browser-style login: no Authorization header, so a CSRF token is required. */
    protected ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .with(realCsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "%s", "password": "%s"}""".formatted(username, password)));
    }

    /** Logs in and returns the JWT from the session cookie; tests send it as a Bearer header (no CSRF needed). */
    protected String token(String username, String password) throws Exception {
        return login(username, password).andExpect(status().isOk())
                .andReturn().getResponse().getCookie(AuthCookies.SESSION_COOKIE).getValue();
    }

    /** Token of an onboarded client after the mandatory change of the one-time password. */
    protected String clientToken(OnboardedClient client) throws Exception {
        String temporaryToken = token(client.username(), client.temporaryPassword());
        return mvc.perform(post("/api/auth/password")
                        .header("Authorization", bearer(temporaryToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "%s", "newPassword": "%s"}"""
                                .formatted(client.temporaryPassword(), CLIENT_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(AuthCookies.SESSION_COOKIE).getValue();
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

    protected record AccountInfo(long id, String rib) {
    }

    protected AccountInfo newAccount(String agentToken, long clientId, String deposit, String currency) throws Exception {
        String body = openAccount(agentToken, clientId,
                "\"initialDeposit\": " + deposit + ", \"currency\": \"" + currency + "\"")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(body, "$.id");
        return new AccountInfo(id.longValue(), JsonPath.read(body, "$.rib"));
    }

    protected AccountInfo newAccount(String agentToken, long clientId, String deposit) throws Exception {
        return newAccount(agentToken, clientId, deposit, "MAD");
    }

    /** Connected as the application's restricted role, like the application itself. */
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected PostgreSQLContainer postgres;

    /** Connected as the schema owner (the role Flyway uses), to test protections that do not rely on grants. */
    protected JdbcTemplate ownerJdbc() {
        return new JdbcTemplate(new SingleConnectionDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), true));
    }

    protected BigDecimal balanceOf(long accountId) {
        return jdbc.queryForObject("select balance from bank_account where id = ?", BigDecimal.class, accountId);
    }

    /**
     * Global ledger invariants, over every row written by any test so far:
     * every cached balance matches its ledger, and every transfer has exactly one DEBIT and one CREDIT
     * of the same amount as the transfer.
     */
    protected void assertBooksBalanced() {
        List<Long> unreconciled = jdbc.queryForList("""
                select a.id from bank_account a
                left join (select account_id, sum(case when entry_type = 'DEBIT' then -amount else amount end) total
                           from ledger_entry group by account_id) l on l.account_id = a.id
                where a.balance <> coalesce(l.total, 0)""", Long.class);
        List<Long> unbalancedTransfers = jdbc.queryForList("""
                select t.id from transfer t
                left join ledger_entry d on d.transfer_id = t.id and d.entry_type = 'DEBIT'
                left join ledger_entry c on c.transfer_id = t.id and c.entry_type = 'CREDIT'
                where d.id is null or c.id is null or d.amount <> t.amount or c.amount <> t.amount
                   or d.account_id <> t.source_account_id or c.account_id <> t.destination_account_id
                   or (select count(*) from ledger_entry e where e.transfer_id = t.id) <> 2""", Long.class);
        assertThat(unreconciled).as("accounts whose balance != ledger").isEmpty();
        assertThat(unbalancedTransfers).as("transfers with unbalanced entries").isEmpty();
    }
}
