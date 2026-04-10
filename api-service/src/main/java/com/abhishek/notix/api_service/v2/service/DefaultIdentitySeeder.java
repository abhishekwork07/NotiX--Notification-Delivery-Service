package com.abhishek.notix.api_service.v2.service;

import com.abhishek.notix.api_service.v2.model.MembershipRole;
import com.abhishek.notix.api_service.v2.model.PlatformUser;
import com.abhishek.notix.api_service.v2.model.Tenant;
import com.abhishek.notix.api_service.v2.model.TenantMembership;
import com.abhishek.notix.api_service.v2.repo.PlatformUserRepository;
import com.abhishek.notix.api_service.v2.repo.TenantMembershipRepository;
import com.abhishek.notix.api_service.v2.repo.TenantRepository;
import com.abhishek.notix.api_service.v2.security.PasswordHashing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Component
public class DefaultIdentitySeeder implements ApplicationRunner {

    private final TenantRepository tenantRepository;
    private final PlatformUserRepository platformUserRepository;
    private final TenantMembershipRepository tenantMembershipRepository;

    @Value("${notix.defaults.tenant.name:NotiX Default Tenant}")
    private String defaultTenantName;

    @Value("${notix.defaults.tenant.domain:default.notix.local}")
    private String defaultTenantDomain;

    @Value("${notix.defaults.users.admin.username:admin}")
    private String adminUsername;

    @Value("${notix.defaults.users.admin.email:admin@notix.local}")
    private String adminEmail;

    @Value("${notix.defaults.users.admin.password:admin123}")
    private String adminPassword;

    @Value("${notix.defaults.users.operator.username:operator}")
    private String operatorUsername;

    @Value("${notix.defaults.users.operator.email:operator@notix.local}")
    private String operatorEmail;

    @Value("${notix.defaults.users.operator.password:operator123}")
    private String operatorPassword;

    public DefaultIdentitySeeder(TenantRepository tenantRepository,
                                 PlatformUserRepository platformUserRepository,
                                 TenantMembershipRepository tenantMembershipRepository) {
        this.tenantRepository = tenantRepository;
        this.platformUserRepository = platformUserRepository;
        this.tenantMembershipRepository = tenantMembershipRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Tenant defaultTenant = tenantRepository.findByDomain(normalizeDomain(defaultTenantDomain))
                .orElseGet(this::createDefaultTenant);

        PlatformUser admin = upsertLocalUser(
                adminUsername,
                adminEmail,
                "local:admin",
                "Default Admin",
                adminPassword,
                true
        );
        ensureMembership(defaultTenant.getId(), admin, MembershipRole.TENANT_ADMIN);

        PlatformUser operator = upsertLocalUser(
                operatorUsername,
                operatorEmail,
                "local:operator",
                "Default Operator",
                operatorPassword,
                false
        );
        ensureMembership(defaultTenant.getId(), operator, MembershipRole.TENANT_MEMBER);
    }

    private Tenant createDefaultTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName(defaultTenantName);
        tenant.setDomain(normalizeDomain(defaultTenantDomain));
        return tenantRepository.save(tenant);
    }

    private PlatformUser upsertLocalUser(String username,
                                         String email,
                                         String externalUserId,
                                         String displayName,
                                         String defaultPassword,
                                         boolean platformAdmin) {
        PlatformUser user = platformUserRepository.findByUsernameIgnoreCase(username)
                .or(() -> platformUserRepository.findByEmailIgnoreCase(email))
                .or(() -> platformUserRepository.findByExternalUserId(externalUserId))
                .orElseGet(PlatformUser::new);

        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }

        user.setUsername(username.toLowerCase(Locale.ROOT));
        user.setEmail(email.toLowerCase(Locale.ROOT));
        user.setExternalUserId(externalUserId);
        user.setDisplayName(displayName);
        user.setAuthProvider("LOCAL");
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            user.setPasswordHash(PasswordHashing.hashPassword(defaultPassword));
        }
        user.setPlatformAdmin(platformAdmin);
        user.setActive(true);
        return platformUserRepository.save(user);
    }

    private void ensureMembership(UUID tenantId, PlatformUser user, MembershipRole role) {
        TenantMembership membership = tenantMembershipRepository
                .findByTenantIdAndPlatformUserId(tenantId, user.getId())
                .orElseGet(TenantMembership::new);

        if (membership.getId() == null) {
            membership.setId(UUID.randomUUID());
            membership.setTenantId(tenantId);
            membership.setPlatformUserId(user.getId());
        }
        membership.setRole(role);
        membership.setActive(true);
        tenantMembershipRepository.save(membership);
    }

    private String normalizeDomain(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
