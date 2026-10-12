package com.example.ebank.audit;

import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

/**
 * Records audit events. Two transaction modes, on purpose:
 * <ul>
 *   <li>{@link #record}: joins the business transaction, so an event exists only if the operation committed
 *       (no "transfer completed" event for a transfer that was rolled back).</li>
 *   <li>{@link #recordIndependently}: commits on its own, for security events of operations that fail
 *       (a failed login must be kept even though the login throws).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository repository;

    public record AuditEventResponse(Long id, AuditEventType type, String actorUsername, String target,
                                     String ipAddress, String details, Instant createdAt) {
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AppUserPrincipal actor, AuditEventType type, String target, String details) {
        repository.save(event(actor.organizationId(), actor.userId(), actor.username(), type, target, details));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependently(Long organizationId, Long userId, String username, AuditEventType type,
                                    String details) {
        repository.save(event(organizationId, userId, username, type, null, details));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> list(Long organizationId, AuditEventType type, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<AuditEvent> result = type == null
                ? repository.findByOrganizationId(organizationId, pageable)
                : repository.findByOrganizationIdAndEventType(organizationId, type, pageable);
        return new PageResponse<>(
                result.getContent().stream().map(e -> new AuditEventResponse(e.getId(), e.getEventType(),
                        e.getActorUsername(), e.getTarget(), e.getIpAddress(), e.getDetails(), e.getCreatedAt())).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private static AuditEvent event(Long organizationId, Long userId, String username, AuditEventType type,
                                    String target, String details) {
        AuditEvent event = new AuditEvent();
        event.setOrganizationId(organizationId);
        event.setActorUserId(userId);
        event.setActorUsername(username == null ? null : truncate(username, 254));
        event.setEventType(type);
        event.setTarget(target);
        event.setIpAddress(currentClientIp());
        event.setDetails(details == null ? null : truncate(details, 255));
        return event;
    }

    /** Client IP of the current HTTP request (X-Forwarded-For is resolved by Tomcat from trusted proxies only). */
    private static String currentClientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return truncate(request.getRemoteAddr(), 45);
        }
        return null;
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
