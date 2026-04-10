package com.abhishek.notix.api_service.v2.security;

import com.abhishek.notix.api_service.v2.model.MembershipRole;

import java.util.UUID;

public class AuthenticatedActor {

    private UUID tenantId;
    private UUID platformUserId;
    private UUID apiKeyId;
    private MembershipRole membershipRole;
    private boolean platformAdmin;

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getPlatformUserId() {
        return platformUserId;
    }

    public void setPlatformUserId(UUID platformUserId) {
        this.platformUserId = platformUserId;
    }

    public UUID getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(UUID apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public MembershipRole getMembershipRole() {
        return membershipRole;
    }

    public void setMembershipRole(MembershipRole membershipRole) {
        this.membershipRole = membershipRole;
    }

    public boolean isPlatformAdmin() {
        return platformAdmin;
    }

    public void setPlatformAdmin(boolean platformAdmin) {
        this.platformAdmin = platformAdmin;
    }

    public boolean isTenantAdmin() {
        return platformAdmin || membershipRole == MembershipRole.TENANT_ADMIN;
    }

    public String actorType() {
        if (apiKeyId != null) {
            return "API_KEY";
        }
        if (platformUserId != null) {
            return "USER";
        }
        return "SYSTEM";
    }

    public String actorId() {
        if (apiKeyId != null) {
            return apiKeyId.toString();
        }
        if (platformUserId != null) {
            return platformUserId.toString();
        }
        return "system";
    }
}
