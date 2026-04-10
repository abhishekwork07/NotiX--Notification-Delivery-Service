package com.abhishek.notix.api_service.v2.repo;

import com.abhishek.notix.api_service.v2.model.TenantMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    Optional<TenantMembership> findByTenantIdAndPlatformUserId(UUID tenantId, UUID platformUserId);

    Optional<TenantMembership> findByTenantIdAndPlatformUserIdAndActiveTrue(UUID tenantId, UUID platformUserId);

    boolean existsByTenantIdAndPlatformUserId(UUID tenantId, UUID platformUserId);

    List<TenantMembership> findByPlatformUserIdAndActiveTrue(UUID platformUserId);
}
