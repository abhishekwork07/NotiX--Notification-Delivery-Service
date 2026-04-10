package com.abhishek.notix.api_service.security;

import com.abhishek.notix.api_service.v2.repo.PlatformUserRepository;
import com.abhishek.notix.api_service.v2.security.JwtTokenService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiKeyAuthFilter implements Filter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    @Value("${notix.security.api-key}")
    private String validApiKey;

    private static final String HEADER_NAME = "X-API-KEY";
    private static final String ALT_HEADER_NAME = "X-NOTIX-API-KEY";

    private final JwtTokenService jwtTokenService;
    private final PlatformUserRepository platformUserRepository;

    public ApiKeyAuthFilter(JwtTokenService jwtTokenService,
                            PlatformUserRepository platformUserRepository) {
        this.jwtTokenService = jwtTokenService;
        this.platformUserRepository = platformUserRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String authorization = httpRequest.getHeader(AUTHORIZATION_HEADER);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length()).trim();
            JwtTokenService.JwtClaims claims = jwtTokenService.verify(token);
            if (claims != null && platformUserRepository.findById(claims.platformUserId())
                    .map(user -> user.isActive())
                    .orElse(false)) {
                chain.doFilter(request, response);
                return;
            }
        }

        String incomingKey = httpRequest.getHeader(HEADER_NAME);
        if (incomingKey == null || incomingKey.isBlank()) {
            incomingKey = httpRequest.getHeader(ALT_HEADER_NAME);
        }

        if (validApiKey.equals(incomingKey)) {
            chain.doFilter(request, response); // Authorized
        } else {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("Invalid API Key!!!!");
        }
    }
}
