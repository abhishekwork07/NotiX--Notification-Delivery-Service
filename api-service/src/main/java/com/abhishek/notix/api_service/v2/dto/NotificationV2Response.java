package com.abhishek.notix.api_service.v2.dto;

import com.abhishek.notix.common.enums.Channel;
import com.abhishek.notix.common.enums.Status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NotificationV2Response(
        UUID id,
        UUID tenantId,
        String recipient,
        Channel channel,
        Status status,
        String template,
        UUID templateId,
        UUID providerAccountId,
        String subject,
        String body,
        String idempotencyKey,
        Instant scheduledAt,
        List<DeliveryAttemptResponse> attempts
) {
}
