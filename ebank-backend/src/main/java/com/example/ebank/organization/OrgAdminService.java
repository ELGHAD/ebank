package com.example.ebank.organization;

import com.example.ebank.audit.AuditEventType;
import com.example.ebank.audit.AuditService;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.entities.AccountStatus;
import com.example.ebank.entities.AppRole;
import com.example.ebank.entities.AppUser;
import com.example.ebank.entities.Organization;
import com.example.ebank.exception.BusinessRuleException;
import com.example.ebank.exception.ConflictException;
import com.example.ebank.exception.NotFoundException;
import com.example.ebank.organization.OrganizationDtos.CreateStaffRequest;
import com.example.ebank.organization.OrganizationDtos.CreateStaffResponse;
import com.example.ebank.organization.OrganizationDtos.CurrencyAmount;
import com.example.ebank.organization.OrganizationDtos.OrganizationOverview;
import com.example.ebank.organization.OrganizationDtos.StaffMember;
import com.example.ebank.repositories.AppRoleRepository;
import com.example.ebank.repositories.AppUserRepository;
import com.example.ebank.repositories.BankAccountRepository;
import com.example.ebank.repositories.ClientRepository;
import com.example.ebank.repositories.OrganizationRepository;
import com.example.ebank.services.TemporaryPasswords;
import com.example.ebank.transfer.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Administration of one organization by its ORG_ADMINs. Every operation is scoped to the caller's organization. */
@Service
@RequiredArgsConstructor
public class OrgAdminService {

    private static final Set<String> STAFF_ROLES = Set.of(AppRole.AGENT, AppRole.ORG_ADMIN);

    private final OrganizationRepository organizationRepo;
    private final AppUserRepository userRepo;
    private final AppRoleRepository roleRepo;
    private final ClientRepository clientRepo;
    private final BankAccountRepository accountRepo;
    private final TransferRepository transferRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public OrganizationOverview overview(AppUserPrincipal admin) {
        Long orgId = admin.organizationId();
        Organization organization = organizationRepo.findById(orgId).orElseThrow();

        Map<AccountStatus, Long> byStatus = new EnumMap<>(AccountStatus.class);
        for (Object[] row : accountRepo.countByStatus(orgId)) {
            byStatus.put((AccountStatus) row[0], (Long) row[1]);
        }
        List<CurrencyAmount> balances = accountRepo.sumBalancesByCurrency(orgId).stream()
                .map(row -> new CurrencyAmount((String) row[0], (BigDecimal) row[1])).toList();

        long transfers = 0;
        List<CurrencyAmount> volume = new ArrayList<>();
        for (Object[] row : transferRepo.statsSince(orgId, Instant.now().minus(Duration.ofDays(30)))) {
            transfers += (Long) row[1];
            volume.add(new CurrencyAmount((String) row[0], (BigDecimal) row[2]));
        }
        long accounts = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new OrganizationOverview(organization.getName(), clientRepo.countByOrganizationId(orgId), accounts,
                byStatus.getOrDefault(AccountStatus.ACTIVE, 0L), byStatus.getOrDefault(AccountStatus.BLOCKED, 0L),
                byStatus.getOrDefault(AccountStatus.CLOSED, 0L), balances, transfers, volume,
                userRepo.findByOrganizationAndRoles(orgId, STAFF_ROLES).size());
    }

    @Transactional(readOnly = true)
    public List<StaffMember> listStaff(AppUserPrincipal admin) {
        return userRepo.findByOrganizationAndRoles(admin.organizationId(), STAFF_ROLES).stream()
                .map(OrgAdminService::toStaffMember).toList();
    }

    @Transactional
    public CreateStaffResponse createStaff(AppUserPrincipal admin, CreateStaffRequest req) {
        String username = req.email().trim().toLowerCase(Locale.ROOT);
        if (userRepo.existsByUsername(username)) {
            throw new ConflictException("EMAIL_TAKEN", "A user with this email already exists");
        }
        String temporaryPassword = TemporaryPasswords.generate();
        AppUser user = new AppUser();
        user.setOrganization(organizationRepo.getReferenceById(admin.organizationId()));
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.getRoles().add(roleRepo.findByRoleName(req.role()).orElseThrow());
        userRepo.saveAndFlush(user);

        audit.record(admin, AuditEventType.STAFF_CREATED, "user:" + user.getId(), req.role() + " " + username);
        return new CreateStaffResponse(toStaffMember(user), temporaryPassword);
    }

    /**
     * Enables or disables a staff member (takes effect on their next request). The organization row is locked so
     * that two administrators disabling each other at the same moment cannot leave the organization without one.
     */
    @Transactional
    public StaffMember setEnabled(AppUserPrincipal admin, Long userId, boolean enabled) {
        organizationRepo.findByIdForUpdate(admin.organizationId()).orElseThrow();
        AppUser user = userRepo.findByIdAndOrganizationId(userId, admin.organizationId())
                .filter(u -> u.getRoles().stream().anyMatch(r -> STAFF_ROLES.contains(r.getRoleName())))
                .orElseThrow(() -> new NotFoundException("STAFF_NOT_FOUND", "Staff member not found"));
        if (user.getId().equals(admin.userId())) {
            throw new BusinessRuleException("CANNOT_CHANGE_OWN_ACCOUNT", "You cannot disable or enable your own account");
        }
        if (user.isEnabled() == enabled) {
            return toStaffMember(user);
        }
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> AppRole.ORG_ADMIN.equals(r.getRoleName()));
        if (!enabled && isAdmin && userRepo.countEnabledWithRole(admin.organizationId(), AppRole.ORG_ADMIN) <= 1) {
            throw new BusinessRuleException("LAST_ORG_ADMIN", "The organization must keep at least one active administrator");
        }
        user.setEnabled(enabled);
        audit.record(admin, AuditEventType.STAFF_ENABLED_CHANGED, "user:" + user.getId(),
                (enabled ? "enabled " : "disabled ") + user.getUsername());
        return toStaffMember(user);
    }

    private static StaffMember toStaffMember(AppUser u) {
        return new StaffMember(u.getId(), u.getUsername(),
                u.getRoles().stream().map(AppRole::getRoleName).sorted().toList(),
                u.isEnabled(), u.isMustChangePassword(), u.getCreatedAt());
    }
}
