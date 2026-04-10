package com.abhishek.notix.api_service.v2.security;

import com.abhishek.notix.api_service.v2.model.ApiKey;
import com.abhishek.notix.api_service.v2.model.PlatformUser;
import com.abhishek.notix.api_service.v2.model.TenantMembership;
import com.abhishek.notix.api_service.v2.repo.ApiKeyRepository;
import com.abhishek.notix.api_service.v2.repo.PlatformUserRepository;
import com.abhishek.notix.api_service.v2.repo.TenantMembershipRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class V2AuthFilter implements Filter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String API_KEY_HEADER = "X-NOTIX-API-KEY";
    private static final String USER_HEADER = "X-NOTIX-EXTERNAL-USER-ID";
    private static final String TENANT_HEADER = "X-NOTIX-TENANT-ID";

    private final ApiKeyRepository apiKeyRepository;
    private final PlatformUserRepository platformUserRepository;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final JwtTokenService jwtTokenService;

    public V2AuthFilter(ApiKeyRepository apiKeyRepository,
                        PlatformUserRepository platformUserRepository,
                        TenantMembershipRepository tenantMembershipRepository,
                        JwtTokenService jwtTokenService) {
        this.apiKeyRepository = apiKeyRepository;
        this.platformUserRepository = platformUserRepository;
        this.tenantMembershipRepository = tenantMembershipRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!httpRequest.getRequestURI().startsWith("/v2/")) {
            chain.doFilter(request, response);
            return;
        }

        if (httpRequest.getRequestURI().equals("/v2/tenants")
                && HttpMethod.POST.matches(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        if (httpRequest.getRequestURI().equals("/v2/auth/login")
                && HttpMethod.POST.matches(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        try {
            AuthenticatedActor actor = authenticate(httpRequest);
            if (actor == null) {
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid v2 credentials");
                return;
            }
            TenantRequestContextHolder.set(actor);
            chain.doFilter(request, response);
        } finally {
            TenantRequestContextHolder.clear();
        }
    }

    private AuthenticatedActor authenticate(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length()).trim();
            JwtTokenService.JwtClaims claims = jwtTokenService.verify(token);
            if (claims == null) {
                return null;
            }

            Optional<PlatformUser> userOpt = platformUserRepository.findById(claims.platformUserId());
            if (userOpt.isEmpty() || !userOpt.get().isActive()) {
                return null;
            }

            PlatformUser user = userOpt.get();
            AuthenticatedActor actor = new AuthenticatedActor();
            actor.setTenantId(claims.tenantId());
            actor.setPlatformUserId(user.getId());
            actor.setPlatformAdmin(user.isPlatformAdmin());

            Optional<TenantMembership> membership = tenantMembershipRepository
                    .findByTenantIdAndPlatformUserIdAndActiveTrue(claims.tenantId(), user.getId());
            if (membership.isPresent()) {
                actor.setMembershipRole(membership.get().getRole());
                return actor;
            }

            if (user.isPlatformAdmin()) {
                return actor;
            }
            return null;
        }

        String rawApiKey = request.getHeader(API_KEY_HEADER);
        if (rawApiKey != null && !rawApiKey.isBlank()) {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByHashedKeyAndActiveTrue(CredentialHashing.sha256(rawApiKey));
            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();
                apiKey.setLastUsedAt(Instant.now());
                apiKeyRepository.save(apiKey);

                AuthenticatedActor actor = new AuthenticatedActor();
                actor.setTenantId(apiKey.getTenantId());
                actor.setApiKeyId(apiKey.getId());
                return actor;
            }
            return null;
        }

        String externalUserId = request.getHeader(USER_HEADER);
        String tenantHeader = request.getHeader(TENANT_HEADER);
        if (externalUserId == null || externalUserId.isBlank() || tenantHeader == null || tenantHeader.isBlank()) {
            return null;
        }

        UUID tenantId;
        try {
            tenantId = UUID.fromString(tenantHeader);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        Optional<PlatformUser> userOpt = platformUserRepository.findByExternalUserId(externalUserId);
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            return null;
        }

        PlatformUser user = userOpt.get();
        AuthenticatedActor actor = new AuthenticatedActor();
        actor.setTenantId(tenantId);
        actor.setPlatformUserId(user.getId());
        actor.setPlatformAdmin(user.isPlatformAdmin());

        Optional<TenantMembership> membership = tenantMembershipRepository.findByTenantIdAndPlatformUserIdAndActiveTrue(tenantId, user.getId());
        if (membership.isPresent()) {
            actor.setMembershipRole(membership.get().getRole());
            return actor;
        }

        if (user.isPlatformAdmin()) {
            return actor;
        }

        return null;
    }
}
