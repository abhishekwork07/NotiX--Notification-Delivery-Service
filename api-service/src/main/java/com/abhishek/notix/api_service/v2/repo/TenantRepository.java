package com.abhishek.notix.api_service.v2.repo;

import com.abhishek.notix.api_service.v2.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByDomain(String domain);
}
