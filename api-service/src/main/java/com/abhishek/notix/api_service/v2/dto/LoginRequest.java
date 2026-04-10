package com.abhishek.notix.api_service.v2.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record LoginRequest(
        String login,
        @Email String email,
        @NotBlank String password,
        UUID tenantId
) {
}
