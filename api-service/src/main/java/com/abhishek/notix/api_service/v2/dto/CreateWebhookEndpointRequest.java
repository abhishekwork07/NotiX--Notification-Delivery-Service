package com.abhishek.notix.api_service.v2.dto;

import com.abhishek.notix.common.enums.NotificationLifecycleEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record CreateWebhookEndpointRequest(
        @NotBlank String name,
        @NotBlank String url,
        @NotEmpty Set<NotificationLifecycleEventType> subscribedEvents
) {
}
