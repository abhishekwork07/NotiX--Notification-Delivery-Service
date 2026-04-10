package com.abhishek.notix.api_service.v2.service;

import com.abhishek.notix.api_service.v2.dto.JwtAuthResponse;
import com.abhishek.notix.api_service.v2.dto.JwtLoginRequest;
import com.abhishek.notix.api_service.v2.dto.LoginRequest;
import com.abhishek.notix.api_service.v2.model.PlatformUser;
import com.abhishek.notix.api_service.v2.model.TenantMembership;
import com.abhishek.notix.api_service.v2.repo.PlatformUserRepository;
import com.abhishek.notix.api_service.v2.repo.TenantMembershipRepository;
import com.abhishek.notix.api_service.v2.security.JwtTokenService;
import com.abhishek.notix.api_service.v2.security.PasswordHashing;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LocalJwtAuthService {

    private final PlatformUserRepository platformUserRepository;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final JwtTokenService jwtTokenService;

    public LocalJwtAuthService(PlatformUserRepository platformUserRepository,
                               TenantMembershipRepository tenantMembershipRepository,
                               JwtTokenService jwtTokenService) {
        this.platformUserRepository = platformUserRepository;
        this.tenantMembershipRepository = tenantMembershipRepository;
        this.jwtTokenService = jwtTokenService;
    }

    public JwtAuthResponse login(JwtLoginRequest request) {
        return issueTokenForLocalUser(request.email(), request.password(), request.tenantId());
    }

    public JwtAuthResponse login(LoginRequest request) {
        String principal = request.login();
        if (principal == null || principal.isBlank()) {
            principal = request.email();
        }
        if (principal == null || principal.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "login or email is required");
        }
        return issueTokenForLocalUser(principal, request.password(), request.tenantId());
    }

    private JwtAuthResponse issueTokenForLocalUser(String login, String rawPassword, UUID requestedTenantId) {
        PlatformUser user = resolveUser(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username/email or password"));

        if (!user.isActive() || !"LOCAL".equalsIgnoreCase(user.getAuthProvider())
                || user.getPasswordHash() == null || !PasswordHashing.matches(rawPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username/email or password");
        }

        TenantMembership membership = resolveMembership(user, requestedTenantId);

        JwtTokenService.TokenEnvelope token = jwtTokenService.issueToken(
                membership.getTenantId(),
                user.getId(),
                membership.getRole(),
                user.isPlatformAdmin()
        );

        return new JwtAuthResponse(
                token.token(),
                "Bearer",
                token.expiresAt(),
                membership.getTenantId(),
                user.getId(),
                membership.getRole().name(),
                user.isPlatformAdmin()
        );
    }

    private Optional<PlatformUser> resolveUser(String login) {
        Optional<PlatformUser> byUsername = platformUserRepository.findByUsernameIgnoreCase(login);
        if (byUsername.isPresent()) {
            return byUsername;
        }
        return platformUserRepository.findByEmailIgnoreCase(login);
    }

    private TenantMembership resolveMembership(PlatformUser user, UUID requestedTenantId) {
        if (requestedTenantId != null) {
            return tenantMembershipRepository
                    .findByTenantIdAndPlatformUserIdAndActiveTrue(requestedTenantId, user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of the tenant"));
        }

        List<TenantMembership> memberships = tenantMembershipRepository.findByPlatformUserIdAndActiveTrue(user.getId());
        if (memberships.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User has no active tenant membership");
        }
        if (memberships.size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId is required when the user belongs to multiple tenants");
        }
        return memberships.get(0);
    }
}
