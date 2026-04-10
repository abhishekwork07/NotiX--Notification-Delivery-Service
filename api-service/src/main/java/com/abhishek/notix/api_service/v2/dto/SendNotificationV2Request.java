package com.abhishek.notix.api_service.v2.dto;

import com.abhishek.notix.common.enums.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SendNotificationV2Request(
        @NotBlank String to,
        @NotNull Channel channel,
        UUID templateId,
        String subject,
        String body,
        Map<String, Object> params,
        String idempotencyKey,
        Instant scheduledAt,
        UUID providerAccountId
) {
}
