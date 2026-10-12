package com.example.ebank.repositories;

import com.example.ebank.entities.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {

    boolean existsByOrganizationIdAndIdentityRef(Long organizationId, String identityRef);

    // Tenant-scoped lookup: a client of another organization is reported as "not found".
    Optional<Client> findByIdAndOrganizationId(Long id, Long organizationId);

    // The list shows the login email: fetch the user in the same query (no N+1)
    @Override
    @EntityGraph(attributePaths = "appUser")
    Page<Client> findAll(Specification<Client> spec, Pageable pageable);
}
