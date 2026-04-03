package com.abhishek.notix.retry_scheduler_service.repo;

import com.abhishek.notix.common.enums.Status;
import com.abhishek.notix.retry_scheduler_service.model.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {

    @Query("SELECT d FROM DeliveryLog d WHERE d.status = 'FAILED' AND d.attemptNo < :maxRetries")
    List<DeliveryLog> findRetryableMessages(@Param("maxRetries") int maxRetries);

    @Query("""
            SELECT d FROM DeliveryLog d
            WHERE d.status = com.abhishek.notix.common.enums.Status.FAILED
              AND d.attemptNo < :maxRetries
              AND d.attemptNo = (
                  SELECT MAX(innerLog.attemptNo)
                  FROM DeliveryLog innerLog
                  WHERE innerLog.notificationId = d.notificationId
              )
            """)
    List<DeliveryLog> findLatestFailedAttempts(@Param("maxRetries") int maxRetries);

    @Query("""
            SELECT d FROM DeliveryLog d
            WHERE d.status = com.abhishek.notix.common.enums.Status.FAILED
              AND d.attemptNo >= :maxRetries
              AND d.attemptNo = (
                  SELECT MAX(innerLog.attemptNo)
                  FROM DeliveryLog innerLog
                  WHERE innerLog.notificationId = d.notificationId
              )
            """)
    List<DeliveryLog> findTerminalFailures(@Param("maxRetries") int maxRetries);

    List<DeliveryLog> findByStatus(Status status);

    boolean existsByNotificationIdAndAttemptNo(UUID notificationId, int attemptNo);

    Optional<DeliveryLog> findByNotificationIdAndAttemptNo(UUID notificationId, int attemptNo);
}
