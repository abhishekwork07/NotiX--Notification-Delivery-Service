package com.abhishek.notix.api_service.v2.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateApiKeyRequest(@NotBlank String name) {
}
