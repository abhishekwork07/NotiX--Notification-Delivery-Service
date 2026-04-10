package com.abhishek.notix.api_service.v2.dto;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyResponse(
        UUID id,
        String name,
        String keyPrefix,
        String rawKey,
        Instant createdAt
) {
}
