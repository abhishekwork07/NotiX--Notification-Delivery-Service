package com.abhishek.notix.api_service.v2.dto;

import java.time.Instant;
import java.util.UUID;

public record JwtAuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UUID tenantId,
        UUID platformUserId,
        String role,
        boolean platformAdmin
) {
}
