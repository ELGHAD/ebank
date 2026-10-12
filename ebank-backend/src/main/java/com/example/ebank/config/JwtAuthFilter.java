package com.example.ebank.config;

import com.example.ebank.entities.AppUser;
import com.example.ebank.repositories.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates requests carrying a JWT, either in the HttpOnly session cookie (browsers) or in an
 * {@code Authorization: Bearer} header (API clients).
 * <p>
 * Deliberately not a Spring bean: Boot would otherwise also register it as a servlet filter.
 * The request stays unauthenticated (protected endpoints then answer 401) when the token is invalid or
 * expired, the user is unknown or disabled, or the token was revoked (its version is older than the user's).
 * A user who must change their password is authenticated without any role, so only the account endpoints
 * that need no role (me, password change, logout) are reachable.
 */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserRepository userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            extractToken(request)
                    .flatMap(jwtService::validate)
                    .flatMap(claims -> userRepo.findByUsername(claims.username())
                            .filter(AppUser::isEnabled)
                            .filter(user -> user.getTokenVersion() == claims.version()))
                    .ifPresent(user -> authenticate(user, request));
        }

        filterChain.doFilter(request, response);
    }

    private static Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (AuthCookies.SESSION_COOKIE.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return Optional.of(cookie.getValue());
                }
            }
        }
        return Optional.empty();
    }

    private void authenticate(AppUser user, HttpServletRequest request) {
        List<GrantedAuthority> authorities = user.isMustChangePassword()
                ? List.of()
                : user.getRoles().stream()
                        .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.getRoleName()))
                        .toList();
        Long organizationId = user.getOrganization() == null ? null : user.getOrganization().getId();
        var principal = new AppUserPrincipal(user.getId(), user.getUsername(), organizationId,
                user.isMustChangePassword());

        var authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
