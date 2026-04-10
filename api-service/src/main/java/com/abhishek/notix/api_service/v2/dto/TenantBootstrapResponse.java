package com.abhishek.notix.api_service.v2.dto;

import java.util.UUID;

public record TenantBootstrapResponse(
        UUID tenantId,
        UUID ownerUserId,
        UUID apiKeyId,
        String bootstrapApiKey
) {
}
