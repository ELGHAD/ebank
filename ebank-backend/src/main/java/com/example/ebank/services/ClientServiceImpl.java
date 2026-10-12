package com.example.ebank.services;

import com.example.ebank.audit.AuditEventType;
import com.example.ebank.audit.AuditService;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.Mappers;
import com.example.ebank.dto.OnboardClientRequest;
import com.example.ebank.dto.OnboardClientResponse;
import com.example.ebank.entities.AppRole;
import com.example.ebank.entities.AppUser;
import com.example.ebank.entities.Client;
import com.example.ebank.entities.Organization;
import com.example.ebank.exception.ConflictException;
import com.example.ebank.repositories.AppRoleRepository;
import com.example.ebank.repositories.AppUserRepository;
import com.example.ebank.repositories.ClientRepository;
import com.example.ebank.repositories.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClientRepository clientRepo;
    private final AppUserRepository userRepo;
    private final AppRoleRepository roleRepo;
    private final OrganizationRepository organizationRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    /**
     * Creates the login identity and the client record in one transaction: if any step fails, nothing is kept.
     * The client always joins the agent's organization; the request cannot choose another one.
     */
    @Override
    @Transactional
    public OnboardClientResponse onboardClient(AppUserPrincipal agent, OnboardClientRequest req) {
        String username = req.email().trim().toLowerCase(Locale.ROOT);
        String identityRef = req.identityRef().trim();

        if (clientRepo.existsByOrganizationIdAndIdentityRef(agent.organizationId(), identityRef)) {
            throw new ConflictException("IDENTITY_REF_TAKEN", "A client with this identity reference already exists");
        }
        if (userRepo.existsByUsername(username)) {
            throw new ConflictException("EMAIL_TAKEN", "A user with this email already exists");
        }

        Organization organization = organizationRepo.getReferenceById(agent.organizationId());
        String temporaryPassword = generateTemporaryPassword();

        AppUser user = new AppUser();
        user.setOrganization(organization);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setEnabled(true);
        user.setMustChangePassword(true); // the agent has seen this password
        user.getRoles().add(roleRepo.findByRoleName(AppRole.CLIENT).orElseThrow());
        userRepo.save(user);

        Client client = new Client();
        client.setOrganization(organization);
        client.setAppUser(user);
        client.setIdentityRef(identityRef);
        client.setFirstName(req.firstName().trim());
        client.setLastName(req.lastName().trim());
        client.setBirthDate(req.birthDate());
        client.setAddress(req.address());
        clientRepo.saveAndFlush(client);
        audit.record(agent, AuditEventType.CLIENT_ONBOARDED, "client:" + client.getId(), null);

        return new OnboardClientResponse(Mappers.toClientResponse(client), username, temporaryPassword);
    }

    /** 96 random bits, URL-safe Base64 (16 characters). */
    private static String generateTemporaryPassword() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
