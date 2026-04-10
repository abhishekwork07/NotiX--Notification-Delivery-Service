package com.abhishek.notix.api_service.v2.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TenantBootstrapRequest(
        @NotBlank String name,
        @NotBlank String domain,
        String ownerExternalUserId,
        @Email @NotBlank String ownerEmail,
        @NotBlank String ownerDisplayName,
        String ownerPassword
) {
}
