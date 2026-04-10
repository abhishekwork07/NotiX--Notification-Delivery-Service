package com.abhishek.notix.api_service.v2.repo;

import com.abhishek.notix.api_service.v2.model.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {

    List<WebhookEndpoint> findByTenantIdAndActiveTrue(UUID tenantId);
}
