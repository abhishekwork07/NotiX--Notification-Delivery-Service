package com.abhishek.notix.api_service.v2.dto;

import com.abhishek.notix.common.enums.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTemplateRequest(
        @NotBlank String name,
        @NotNull Channel channel,
        String subjectTemplate,
        @NotBlank String bodyTemplate
) {
}
