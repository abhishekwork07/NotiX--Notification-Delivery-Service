package com.abhishek.notix.api_service.v2.repo;

import com.abhishek.notix.api_service.v2.model.NotificationSchedule;
import com.abhishek.notix.api_service.v2.model.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationScheduleRepository extends JpaRepository<NotificationSchedule, UUID> {

    List<NotificationSchedule> findByStatusAndScheduledAtBefore(ScheduleStatus status, Instant scheduledAt);
}
