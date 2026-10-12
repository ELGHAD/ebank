package com.example.ebank.repositories;

import com.example.ebank.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByOrganizationIdAndIdentityRef(Long organizationId, String identityRef);

    // Tenant-scoped lookup: a client of another organization is reported as "not found".
    Optional<Client> findByIdAndOrganizationId(Long id, Long organizationId);
}
