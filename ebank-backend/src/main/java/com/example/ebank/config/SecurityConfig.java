package com.example.ebank.config;

import com.example.ebank.repositories.AppUserRepository;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppUserRepository userRepo;
    private final JwtService jwtService;
    private final CorsProperties corsProperties;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepo.findByUsername(username)
                .map(user -> User.withUsername(user.getUsername())
                        .password(user.getPasswordHash())
                        .disabled(!user.isEnabled())
                        .authorities(user.getRoles().stream()
                                .map(r -> "ROLE_" + r.getRoleName()) // ROLE_CLIENT, ROLE_AGENT -> hasRole("CLIENT")
                                .toArray(String[]::new))
                        .build())
                // UsernameNotFoundException lets Spring apply its timing-attack mitigation
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
        http
                // Browsers authenticate with an HttpOnly cookie, so state-changing requests need a CSRF token:
                // spa() = XSRF-TOKEN cookie readable by the SPA, echoed in the X-XSRF-TOKEN header (BREACH-safe).
                // Requests carrying an Authorization header are exempt: browsers never add it on their own,
                // so a cross-site page cannot forge such a request.
                .csrf(csrf -> csrf
                        .spa()
                        .ignoringRequestMatchers(request -> request.getHeader("Authorization") != null))
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        // Route 401/403 to GlobalExceptionHandler so they share the problem+json format
                        .authenticationEntryPoint((req, res, e) -> exceptionResolver.resolveException(req, res, null, e))
                        .accessDeniedHandler((req, res, e) -> exceptionResolver.resolveException(req, res, null, e)))
                .authorizeHttpRequests(auth -> auth
                        // Error dispatches must not be re-authorized, otherwise every error turns into 401/403
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/client/**").hasRole("CLIENT")
                        .requestMatchers("/api/admin/**").hasRole("AGENT")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthFilter(jwtService, userRepo), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
