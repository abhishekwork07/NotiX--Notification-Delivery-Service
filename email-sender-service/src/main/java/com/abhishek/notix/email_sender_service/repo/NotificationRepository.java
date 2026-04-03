package com.abhishek.notix.email_sender_service.repo;

import com.abhishek.notix.email_sender_service.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
