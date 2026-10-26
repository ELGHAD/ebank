package com.example.ebank.organization;

import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.PageResponse;
import com.example.ebank.organization.OrganizationDtos.ChangeOrganizationStatusRequest;
import com.example.ebank.organization.OrganizationDtos.ChangeStaffEnabledRequest;
import com.example.ebank.organization.OrganizationDtos.CreateOrganizationRequest;
import com.example.ebank.organization.OrganizationDtos.CreateOrganizationResponse;
import com.example.ebank.organization.OrganizationDtos.CreateStaffRequest;
import com.example.ebank.organization.OrganizationDtos.CreateStaffResponse;
import com.example.ebank.organization.OrganizationDtos.OrganizationOverview;
import com.example.ebank.organization.OrganizationDtos.OrganizationSummary;
import com.example.ebank.organization.OrganizationDtos.StaffMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** HTTP endpoints of the organization module. Path rules in SecurityConfig and @PreAuthorize both apply. */
public final class OrganizationControllers {

    private OrganizationControllers() {
    }

    @RestController
    @RequestMapping("/api/platform/organizations")
    @RequiredArgsConstructor
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public static class PlatformController {

        private final PlatformService platformService;

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public CreateOrganizationResponse create(@AuthenticationPrincipal AppUserPrincipal admin,
                                                 @RequestBody @Valid CreateOrganizationRequest request) {
            return platformService.createOrganization(admin, request);
        }

        @GetMapping
        public PageResponse<OrganizationSummary> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
            return platformService.listOrganizations(page, size);
        }

        @PatchMapping("/{organizationId}/status")
        public OrganizationSummary changeStatus(@AuthenticationPrincipal AppUserPrincipal admin,
                                                @PathVariable Long organizationId,
                                                @RequestBody @Valid ChangeOrganizationStatusRequest request) {
            return platformService.changeStatus(admin, organizationId, request.status());
        }
    }

    @RestController
    @RequestMapping("/api/org")
    @RequiredArgsConstructor
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public static class OrgAdminController {

        private final OrgAdminService orgAdminService;

        @GetMapping("/overview")
        public OrganizationOverview overview(@AuthenticationPrincipal AppUserPrincipal admin) {
            return orgAdminService.overview(admin);
        }

        @GetMapping("/staff")
        public List<StaffMember> staff(@AuthenticationPrincipal AppUserPrincipal admin) {
            return orgAdminService.listStaff(admin);
        }

        @PostMapping("/staff")
        @ResponseStatus(HttpStatus.CREATED)
        public CreateStaffResponse createStaff(@AuthenticationPrincipal AppUserPrincipal admin,
                                               @RequestBody @Valid CreateStaffRequest request) {
            return orgAdminService.createStaff(admin, request);
        }

        @PatchMapping("/staff/{userId}/enabled")
        public StaffMember setEnabled(@AuthenticationPrincipal AppUserPrincipal admin, @PathVariable Long userId,
                                      @RequestBody @Valid ChangeStaffEnabledRequest request) {
            return orgAdminService.setEnabled(admin, userId, request.enabled());
        }
    }
}
