package com.example.ebank.organization;

import com.example.ebank.audit.AuditEventType;
import com.example.ebank.audit.AuditService;
import com.example.ebank.billing.Subscription;
import com.example.ebank.billing.SubscriptionRepository;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.PageResponse;
import com.example.ebank.entities.AppRole;
import com.example.ebank.entities.AppUser;
import com.example.ebank.entities.Organization;
import com.example.ebank.entities.OrganizationStatus;
import com.example.ebank.exception.ConflictException;
import com.example.ebank.exception.NotFoundException;
import com.example.ebank.organization.OrganizationDtos.CreateOrganizationRequest;
import com.example.ebank.organization.OrganizationDtos.CreateOrganizationResponse;
import com.example.ebank.organization.OrganizationDtos.OrganizationSummary;
import com.example.ebank.repositories.AppRoleRepository;
import com.example.ebank.repositories.AppUserRepository;
import com.example.ebank.repositories.BankAccountRepository;
import com.example.ebank.repositories.ClientRepository;
import com.example.ebank.repositories.OrganizationRepository;
import com.example.ebank.services.TemporaryPasswords;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Organization lifecycle, for platform administrators. Deliberately exposes counts only:
 * a platform administrator never sees a tenant's clients, accounts or money.
 */
@Service
@RequiredArgsConstructor
public class PlatformService {

    private final OrganizationRepository organizationRepo;
    private final AppUserRepository userRepo;
    private final AppRoleRepository roleRepo;
    private final ClientRepository clientRepo;
    private final BankAccountRepository accountRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final SubscriptionRepository subscriptionRepo;

    /**
     * Creates the organization and its first administrator in one transaction:
     * a failure (e.g. the e-mail is already used) leaves no half-created organization behind.
     */
    @Transactional
    public CreateOrganizationResponse createOrganization(AppUserPrincipal platformAdmin, CreateOrganizationRequest req) {
        String name = req.name().trim();
        String adminUsername = req.adminEmail().trim().toLowerCase(Locale.ROOT);
        if (organizationRepo.existsByNameIgnoreCase(name)) {
            throw new ConflictException("ORGANIZATION_NAME_TAKEN", "An organization with this name already exists");
        }
        if (userRepo.existsByUsername(adminUsername)) {
            throw new ConflictException("EMAIL_TAKEN", "A user with this email already exists");
        }

        Organization organization = new Organization();
        organization.setName(name);
        organizationRepo.saveAndFlush(organization); // the database trigger creates its Free subscription

        String temporaryPassword = TemporaryPasswords.generate();
        AppUser admin = new AppUser();
        admin.setOrganization(organization);
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        admin.setMustChangePassword(true);
        admin.getRoles().add(roleRepo.findByRoleName(AppRole.ORG_ADMIN).orElseThrow());
        userRepo.saveAndFlush(admin);

        audit.recordFor(organization.getId(), platformAdmin, AuditEventType.ORGANIZATION_CREATED,
                "organization:" + organization.getId(), "first administrator " + adminUsername);
        return new CreateOrganizationResponse(new OrganizationSummary(organization.getId(), organization.getName(),
                organization.getStatus(), organization.getCreatedAt(), 1, 0, 0, "FREE", "ACTIVE"), adminUsername, temporaryPassword);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrganizationSummary> listOrganizations(int page, int size) {
        Page<Organization> result = organizationRepo.findAll(PageRequest.of(page, size, Sort.by("name", "id")));
        List<Long> ids = result.getContent().stream().map(Organization::getId).toList();
        Map<Long, Long> users = ids.isEmpty() ? Map.of() : toMap(userRepo.countByOrganizationIds(ids));
        Map<Long, Long> clients = ids.isEmpty() ? Map.of() : toMap(clientRepo.countByOrganizationIds(ids));
        Map<Long, Long> accounts = ids.isEmpty() ? Map.of() : toMap(accountRepo.countByOrganizationIds(ids));
        Map<Long, Subscription> subscriptions = new HashMap<>();
        if (!ids.isEmpty()) {
            subscriptionRepo.findByOrganizationIdIn(ids).forEach(s -> subscriptions.put(s.getOrganizationId(), s));
        }
        return new PageResponse<>(
                result.getContent().stream().map(o -> new OrganizationSummary(o.getId(), o.getName(), o.getStatus(),
                        o.getCreatedAt(), users.getOrDefault(o.getId(), 0L), clients.getOrDefault(o.getId(), 0L),
                        accounts.getOrDefault(o.getId(), 0L), planOf(subscriptions.get(o.getId())),
                        statusOf(subscriptions.get(o.getId())))).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /** Suspending takes effect immediately: every user of the organization loses access on the next request. */
    @Transactional
    public OrganizationSummary changeStatus(AppUserPrincipal platformAdmin, Long organizationId, OrganizationStatus status) {
        Organization organization = organizationRepo.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("ORGANIZATION_NOT_FOUND", "Organization not found"));
        OrganizationStatus previous = organization.getStatus();
        if (previous != status) {
            organization.setStatus(status);
            audit.recordFor(organizationId, platformAdmin, AuditEventType.ORGANIZATION_STATUS_CHANGED,
                    "organization:" + organizationId, previous + " -> " + status);
        }
        Map<Long, Long> users = toMap(userRepo.countByOrganizationIds(List.of(organizationId)));
        Map<Long, Long> clients = toMap(clientRepo.countByOrganizationIds(List.of(organizationId)));
        Map<Long, Long> accounts = toMap(accountRepo.countByOrganizationIds(List.of(organizationId)));
        return new OrganizationSummary(organization.getId(), organization.getName(), organization.getStatus(),
                organization.getCreatedAt(), users.getOrDefault(organizationId, 0L),
                clients.getOrDefault(organizationId, 0L), accounts.getOrDefault(organizationId, 0L),
                planOf(subscriptionRepo.findById(organizationId).orElse(null)),
                statusOf(subscriptionRepo.findById(organizationId).orElse(null)));
    }

    private static String planOf(Subscription s) {
        return s == null ? null : s.getPlan().getCode();
    }

    private static String statusOf(Subscription s) {
        return s == null ? null : s.getStatus().name();
    }

    private static Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }
}
