package com.abhishek.notix.api_service.dto;

import com.abhishek.notix.api_service.model.DeliveryLog;
import com.abhishek.notix.api_service.model.Notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class StatusResponseDTO {

    private UUID id;
    private String status;
    private List<DeliveryAttempt> attempts;

    public StatusResponseDTO(Notification notification, List<DeliveryLog> logs) {
        this.id = notification.getId();
        this.status = notification.getStatus().name();
        this.attempts = logs.stream()
                .map(log -> new DeliveryAttempt(log.getAttemptNo(), log.getStatus().name(), log.getTimestamp()))
                .collect(Collectors.toList());
    }

    public UUID getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public List<DeliveryAttempt> getAttempts() {
        return attempts;
    }

    public record DeliveryAttempt(int attemptNo, String status, Instant timestamp) {}
}
