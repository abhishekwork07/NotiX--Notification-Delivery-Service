package com.abhishek.notix.api_service.v2.repo;

import com.abhishek.notix.api_service.v2.model.UsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UsageEventRepository extends JpaRepository<UsageEvent, UUID> {

    List<UsageEvent> findByTenantIdAndCreatedAtBetween(UUID tenantId, Instant from, Instant to);
}
