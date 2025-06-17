package com.abhishek.notix.email_sender_service.repo;

import com.abhishek.notix.email_sender_service.model.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {

    boolean existsByNotificationIdAndAttemptNo(UUID notificationId, int attemptNo);
}

