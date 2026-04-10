package com.abhishek.notix.common.dto;

import com.abhishek.notix.common.enums.Channel;
import com.abhishek.notix.common.enums.NotificationLifecycleEventType;
import com.abhishek.notix.common.enums.Status;

import java.time.Instant;
import java.util.UUID;

public class NotificationStatusEvent {

    private UUID tenantId;
    private UUID notificationId;
    private Channel channel;
    private Status status;
    private NotificationLifecycleEventType eventType;
    private int attemptNo;
    private String errorMessage;
    private Instant occurredAt = Instant.now();

    public NotificationStatusEvent() {
    }

    public NotificationStatusEvent(UUID tenantId, UUID notificationId, Channel channel, Status status,
                                   NotificationLifecycleEventType eventType, int attemptNo,
                                   String errorMessage, Instant occurredAt) {
        this.tenantId = tenantId;
        this.notificationId = notificationId;
        this.channel = channel;
        this.status = status;
        this.eventType = eventType;
        this.attemptNo = attemptNo;
        this.errorMessage = errorMessage;
        this.occurredAt = occurredAt;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public NotificationLifecycleEventType getEventType() {
        return eventType;
    }

    public void setEventType(NotificationLifecycleEventType eventType) {
        this.eventType = eventType;
    }

    public int getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(int attemptNo) {
        this.attemptNo = attemptNo;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
