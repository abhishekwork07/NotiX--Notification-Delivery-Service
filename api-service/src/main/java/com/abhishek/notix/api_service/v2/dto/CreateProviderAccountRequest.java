package com.abhishek.notix.api_service.v2.dto;

import com.abhishek.notix.api_service.v2.model.ProviderType;
import com.abhishek.notix.common.enums.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateProviderAccountRequest(
        @NotBlank String name,
        @NotNull Channel channel,
        @NotNull ProviderType providerType,
        @NotNull Map<String, Object> configuration
) {
}
