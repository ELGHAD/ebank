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
 * Creates the demo agent account. Active only with the "demo" profile, and never with a built-in password:
 * the password must come from configuration (env DEMO_AGENT_PASSWORD).
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
        String username = demo.agentUsername().trim().toLowerCase(Locale.ROOT);
        if (userRepo.existsByUsername(username)) {
            return;
        }
        if (demo.agentPassword() == null || demo.agentPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("The demo profile requires app.demo.agent-password (env DEMO_AGENT_PASSWORD) "
                    + "of at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        Organization organization = organizationRepo.findByName(demo.organizationName())
                .orElseThrow(() -> new IllegalStateException("Organization not found: " + demo.organizationName()));

        AppUser agent = new AppUser();
        agent.setOrganization(organization);
        agent.setUsername(username);
        agent.setPasswordHash(passwordEncoder.encode(demo.agentPassword()));
        agent.getRoles().add(roleRepo.findByRoleName(AppRole.AGENT).orElseThrow());
        userRepo.save(agent);
        log.info("Demo agent '{}' created in organization '{}'", username, organization.getName());
    }
}
