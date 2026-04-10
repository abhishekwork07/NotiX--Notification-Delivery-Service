package com.abhishek.notix.api_service.v2.repo;

import com.abhishek.notix.api_service.v2.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
