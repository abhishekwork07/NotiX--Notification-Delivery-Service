package com.abhishek.notix.api_service.v2.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UsageSummaryResponse(
        UUID tenantId,
        Instant from,
        Instant to,
        Map<String, Long> totals
) {
}
