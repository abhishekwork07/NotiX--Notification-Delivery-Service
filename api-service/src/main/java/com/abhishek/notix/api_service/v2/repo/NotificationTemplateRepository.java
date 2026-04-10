package com.abhishek.notix.api_service.v2.repo;

import com.abhishek.notix.api_service.v2.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByIdAndTenantId(UUID id, UUID tenantId);
}
