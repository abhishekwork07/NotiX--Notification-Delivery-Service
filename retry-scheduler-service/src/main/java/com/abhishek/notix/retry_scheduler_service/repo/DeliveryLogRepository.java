package com.abhishek.notix.retry_scheduler_service.repo;

import com.abhishek.notix.common.enums.Status;
import com.abhishek.notix.retry_scheduler_service.model.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {
    List<DeliveryLog> findByStatusAndAttemptNoLessThan(Status status, int maxAttempts);
}

