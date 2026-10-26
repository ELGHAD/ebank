package com.example.ebank.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findTop12ByOrganizationIdOrderByPeriodStartDesc(Long organizationId);

    boolean existsByOrganizationIdAndPeriodStart(Long organizationId, Instant periodStart);
}
