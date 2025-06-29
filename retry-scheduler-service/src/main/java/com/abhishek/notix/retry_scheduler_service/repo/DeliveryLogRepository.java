package com.abhishek.notix.retry_scheduler_service.repo;

import com.abhishek.notix.common.enums.Status;
import com.abhishek.notix.retry_scheduler_service.model.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {

    List<DeliveryLog> findByStatusAndAttemptNoLessThan(Status status, int maxAttempts);

    @Query("SELECT d FROM DeliveryLog d WHERE d.status = 'FAILED' AND d.attemptNo < :maxRetries")
    List<DeliveryLog> findRetryableMessages(@Param("maxRetries") int maxRetries);
}

