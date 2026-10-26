package com.example.ebank.repositories;

import com.example.ebank.entities.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {

    boolean existsByOrganizationIdAndIdentityRef(Long organizationId, String identityRef);

    long countByOrganizationId(Long organizationId);

    /** Rows: [organizationId, count]. */
    @Query("select c.organization.id, count(c) from Client c where c.organization.id in :ids group by c.organization.id")
    List<Object[]> countByOrganizationIds(@Param("ids") Collection<Long> ids);

    // Tenant-scoped lookup: a client of another organization is reported as "not found".
    Optional<Client> findByIdAndOrganizationId(Long id, Long organizationId);

    // The list shows the login email: fetch the user in the same query (no N+1)
    @Override
    @EntityGraph(attributePaths = "appUser")
    Page<Client> findAll(Specification<Client> spec, Pageable pageable);
}
