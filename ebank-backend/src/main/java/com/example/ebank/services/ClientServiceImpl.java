package com.example.ebank.services;

import com.example.ebank.audit.AuditEventType;
import com.example.ebank.audit.AuditService;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.ClientDetailsResponse;
import com.example.ebank.dto.ClientSummaryResponse;
import com.example.ebank.dto.Mappers;
import com.example.ebank.dto.PageResponse;
import com.example.ebank.dto.OnboardClientRequest;
import com.example.ebank.dto.OnboardClientResponse;
import com.example.ebank.entities.AppRole;
import com.example.ebank.entities.AppUser;
import com.example.ebank.entities.Client;
import com.example.ebank.entities.Organization;
import com.example.ebank.exception.ConflictException;
import com.example.ebank.exception.NotFoundException;
import com.example.ebank.repositories.BankAccountRepository;
import com.example.ebank.repositories.AppRoleRepository;
import com.example.ebank.repositories.AppUserRepository;
import com.example.ebank.repositories.ClientRepository;
import com.example.ebank.repositories.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepo;
    private final AppUserRepository userRepo;
    private final AppRoleRepository roleRepo;
    private final OrganizationRepository organizationRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final BankAccountRepository accountRepo;

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
        String temporaryPassword = TemporaryPasswords.generate();

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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClientSummaryResponse> searchClients(AppUserPrincipal agent, String query, int page, int size) {
        Specification<Client> spec = (root, q, cb) -> cb.equal(root.get("organization").get("id"), agent.organizationId());
        if (query != null && !query.isBlank()) {
            // Escape LIKE wildcards so "%" or "_" typed by the agent match literally
            String pattern = "%" + query.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("lastName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("identityRef")), pattern, '\\'),
                    cb.like(root.get("appUser").get("username"), pattern, '\\')));
        }
        Page<Client> result = clientRepo.findAll(spec,
                PageRequest.of(page, size, Sort.by("lastName", "firstName", "id")));

        Map<Long, Long> accountCounts = new HashMap<>();
        List<Long> ids = result.getContent().stream().map(Client::getId).toList();
        if (!ids.isEmpty()) {
            for (Object[] row : accountRepo.countByClientIds(ids)) {
                accountCounts.put((Long) row[0], (Long) row[1]);
            }
        }
        return new PageResponse<>(
                result.getContent().stream().map(c -> new ClientSummaryResponse(c.getId(), c.getIdentityRef(),
                        c.getFirstName(), c.getLastName(), c.getAppUser().getUsername(),
                        accountCounts.getOrDefault(c.getId(), 0L), c.getCreatedAt())).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDetailsResponse getClient(AppUserPrincipal agent, Long clientId) {
        Client client = clientRepo.findByIdAndOrganizationId(clientId, agent.organizationId())
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client not found"));
        return new ClientDetailsResponse(Mappers.toClientResponse(client), client.getAppUser().isMustChangePassword(),
                accountRepo.findByClientIdAndOrganizationIdOrderByIdAsc(clientId, agent.organizationId())
                        .stream().map(Mappers::toAccountResponse).toList());
    }

}
