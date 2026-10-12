package com.example.ebank.audit;

import com.example.ebank.audit.AuditService.AuditEventResponse;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /** Audit trail of the agent's organization, newest first. */
    @GetMapping("/api/admin/audit-events")
    public PageResponse<AuditEventResponse> list(@AuthenticationPrincipal AppUserPrincipal agent,
                                                 @RequestParam(required = false) AuditEventType type,
                                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return auditService.list(agent.organizationId(), type, page, size);
    }
}
