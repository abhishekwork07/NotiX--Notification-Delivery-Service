package com.abhishek.notix.sms_sender_service.model;

import com.abhishek.notix.common.enums.Status;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_logs")
public class DeliveryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;   // just the ID

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    // getters/setters
}

