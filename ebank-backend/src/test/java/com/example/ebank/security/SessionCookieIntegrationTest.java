package com.example.ebank.security;

import com.example.ebank.IntegrationTestSupport;
import com.example.ebank.config.AuthCookies;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Browser sessions: HttpOnly cookie, CSRF protection, revocation on logout. */
class SessionCookieIntegrationTest extends IntegrationTestSupport {

    private Cookie sessionCookie(MockHttpServletResponse response) {
        return response.getCookie(AuthCookies.SESSION_COOKIE);
    }

    @Test
    void loginSetsAHardenedCookieAndNoTokenInTheBody() throws Exception {
        String agent = newAgent(newOrganization());
        MockHttpServletResponse response = login(agent, AGENT_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn().getResponse();

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie)
                .startsWith(AuthCookies.SESSION_COOKIE + "=")
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api", "Max-Age=900");
    }

    @Test
    void cookieAuthenticatesRequests() throws Exception {
        Cookie session = sessionCookie(login(newAgent(newOrganization()), AGENT_PASSWORD).andReturn().getResponse());

        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isOk());
    }

    @Test
    void csrfEndpointIssuesAReadableXsrfCookie() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    // CSRF: a cross-site form can make the browser send the cookie, but cannot read or send the CSRF token
    @Test
    void stateChangingRequestWithCookieRequiresCsrfToken() throws Exception {
        String agentToken = agentToken(newOrganization());
        OnboardedClient client = onboardClient(agentToken);
        Cookie session = sessionCookie(login(client.username(), client.temporaryPassword()).andReturn().getResponse());
        String body = """
                {"currentPassword": "%s", "newPassword": "a-brand-new-password-1"}""".formatted(client.temporaryPassword());

        mvc.perform(post("/api/auth/password").cookie(session).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));

        mvc.perform(post("/api/auth/password").cookie(session).with(realCsrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void loginItselfRequiresCsrfToken() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"x@test.ebank\", \"password\": \"whatever\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    // A request carrying an Authorization header must not fall back to the cookie (that would bypass CSRF)
    @Test
    void invalidBearerHeaderNeverFallsBackToTheCookie() throws Exception {
        Cookie session = sessionCookie(login(newAgent(newOrganization()), AGENT_PASSWORD).andReturn().getResponse());

        mvc.perform(get("/api/auth/me").cookie(session).header("Authorization", "Bearer forged"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheSessionEverywhereAndClearsTheCookie() throws Exception {
        String agent = newAgent(newOrganization());
        Cookie deviceA = sessionCookie(login(agent, AGENT_PASSWORD).andReturn().getResponse());
        String deviceB = token(agent, AGENT_PASSWORD);

        MockHttpServletResponse logout = mvc.perform(post("/api/auth/logout").cookie(deviceA).with(realCsrf()))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();
        assertThat(logout.getHeader("Set-Cookie")).contains(AuthCookies.SESSION_COOKIE + "=;").contains("Max-Age=0");

        // The stolen-or-copied token stops working too
        mvc.perform(get("/api/auth/me").cookie(deviceA)).andExpect(status().isUnauthorized());
        getWithToken("/api/auth/me", deviceB).andExpect(status().isUnauthorized());
        // And a new login works
        login(agent, AGENT_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void logoutWithoutSessionStillClearsTheCookie() throws Exception {
        mvc.perform(post("/api/auth/logout").with(realCsrf()))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
    }
}
