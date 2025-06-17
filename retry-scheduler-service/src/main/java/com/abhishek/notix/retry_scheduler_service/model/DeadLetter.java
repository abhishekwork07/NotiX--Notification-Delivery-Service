package com.abhishek.notix.retry_scheduler_service.model;

import com.abhishek.notix.common.enums.Channel;
import com.abhishek.notix.common.enums.Status;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dead_letters")
public class DeadLetter {
    @Id
    private UUID id;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Column(nullable = false)
    private String template;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // getters/setters
}

