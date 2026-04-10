package com.abhishek.notix.api_service.repo;

import com.abhishek.notix.api_service.model.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {

    boolean existsByNotificationIdAndAttemptNo(UUID notificationId, int attemptNo);

    List<DeliveryLog> findByNotificationIdOrderByAttemptNoAscTimestampAsc(UUID id);

    List<DeliveryLog> findByNotificationIdAndTenantIdOrderByAttemptNoAscTimestampAsc(UUID id, UUID tenantId);
}
