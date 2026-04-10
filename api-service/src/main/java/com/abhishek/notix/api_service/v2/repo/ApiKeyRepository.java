package com.abhishek.notix.api_service.v2.repo;

import com.abhishek.notix.api_service.v2.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByHashedKeyAndActiveTrue(String hashedKey);

    List<ApiKey> findByTenantId(UUID tenantId);
}
