package com.example.ebank.config;

import com.example.ebank.entities.AppRole;
import com.example.ebank.entities.AppUser;
import com.example.ebank.entities.Organization;
import com.example.ebank.repositories.AppRoleRepository;
import com.example.ebank.repositories.AppUserRepository;
import com.example.ebank.repositories.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Creates the demo staff accounts. Active only with the "demo" profile, and never with a built-in password:
 * passwords come from configuration (env DEMO_AGENT_PASSWORD, DEMO_ORG_ADMIN_PASSWORD, DEMO_PLATFORM_ADMIN_PASSWORD).
 */
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final DemoProperties demo;
    private final OrganizationRepository organizationRepo;
    private final AppUserRepository userRepo;
    private final AppRoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Organization organization = organizationRepo.findByName(demo.organizationName())
                .orElseThrow(() -> new IllegalStateException("Organization not found: " + demo.organizationName()));

        createIfMissing(demo.agentUsername(), demo.agentPassword(), "DEMO_AGENT_PASSWORD", AppRole.AGENT, organization, true);
        createIfMissing(demo.orgAdminUsername(), demo.orgAdminPassword(), "DEMO_ORG_ADMIN_PASSWORD",
                AppRole.ORG_ADMIN, organization, false);
        createIfMissing(demo.platformAdminUsername(), demo.platformAdminPassword(), "DEMO_PLATFORM_ADMIN_PASSWORD",
                AppRole.PLATFORM_ADMIN, null, false);
    }

    private void createIfMissing(String rawUsername, String password, String passwordEnv, String role,
                                 Organization organization, boolean required) {
        String username = rawUsername.trim().toLowerCase(Locale.ROOT);
        if (userRepo.existsByUsername(username)) {
            return;
        }
        if (password == null || password.isBlank()) {
            if (required) {
                throw new IllegalStateException("The demo profile requires " + passwordEnv);
            }
            log.info("Demo {} not created: {} is not set", role, passwordEnv);
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(passwordEnv + " must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        AppUser user = new AppUser();
        user.setOrganization(organization);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.getRoles().add(roleRepo.findByRoleName(role).orElseThrow());
        userRepo.save(user);
        log.info("Demo {} '{}' created{}", role, username,
                organization == null ? " (platform level)" : " in organization '" + organization.getName() + "'");
    }
}
