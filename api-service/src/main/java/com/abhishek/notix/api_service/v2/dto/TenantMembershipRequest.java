package com.abhishek.notix.api_service.v2.dto;

import com.abhishek.notix.api_service.v2.model.MembershipRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TenantMembershipRequest(
        String externalUserId,
        @Email @NotBlank String email,
        @NotBlank String displayName,
        @NotNull MembershipRole role,
        String password
) {
}
