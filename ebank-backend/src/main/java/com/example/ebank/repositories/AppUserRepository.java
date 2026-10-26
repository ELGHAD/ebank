package com.example.ebank.repositories;

import com.example.ebank.entities.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    // Used by the security filter outside any transaction: roles and organization (status) are needed
    @EntityGraph(attributePaths = {"roles", "organization"})
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);

    Optional<AppUser> findByIdAndOrganizationId(Long id, Long organizationId);

    /** Users of an organization holding at least one of the given roles (staff list). */
    @Query("select distinct u from AppUser u join u.roles r where u.organization.id = :organizationId "
            + "and r.roleName in :roles order by u.username")
    List<AppUser> findByOrganizationAndRoles(@Param("organizationId") Long organizationId,
                                            @Param("roles") Collection<String> roles);

    @Query("select count(distinct u) from AppUser u join u.roles r where u.organization.id = :organizationId "
            + "and r.roleName = :role and u.enabled = true")
    long countEnabledWithRole(@Param("organizationId") Long organizationId, @Param("role") String role);

    /** Enabled staff (agents and organization admins) of an organization: what the STAFF quota counts. */
    @Query("select count(distinct u) from AppUser u join u.roles r where u.organization.id = :organizationId "
            + "and r.roleName in ('AGENT', 'ORG_ADMIN') and u.enabled = true")
    long countEnabledStaff(@Param("organizationId") Long organizationId);

    /** Rows: [organizationId, count]. */
    @Query("select u.organization.id, count(u) from AppUser u where u.organization.id in :ids group by u.organization.id")
    List<Object[]> countByOrganizationIds(@Param("ids") Collection<Long> ids);
}
