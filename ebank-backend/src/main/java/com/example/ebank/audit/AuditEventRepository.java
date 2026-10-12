package com.example.ebank.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Page<AuditEvent> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<AuditEvent> findByOrganizationIdAndEventType(Long organizationId, AuditEventType type, Pageable pageable);
}
