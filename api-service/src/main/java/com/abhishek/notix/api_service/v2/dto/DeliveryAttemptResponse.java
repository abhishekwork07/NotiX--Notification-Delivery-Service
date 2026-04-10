package com.abhishek.notix.api_service.v2.dto;

import com.abhishek.notix.common.enums.Channel;
import com.abhishek.notix.common.enums.Status;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAttemptResponse(
        int attemptNo,
        Status status,
        String errorMessage,
        Instant timestamp,
        Channel channel,
        UUID providerAccountId
) {
}
