package com.abhishek.notix.api_service.v2.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record JwtLoginRequest(
        @NotNull UUID tenantId,
        @Email @NotBlank String email,
        @NotBlank String password
) {
}
