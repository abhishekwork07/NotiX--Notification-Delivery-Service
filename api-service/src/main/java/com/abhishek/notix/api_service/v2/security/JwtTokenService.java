package com.abhishek.notix.api_service.v2.security;

import com.abhishek.notix.api_service.v2.model.MembershipRole;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;

    @Value("${notix.security.jwt.secret}")
    private String jwtSecret;

    @Value("${notix.security.jwt.expiry-minutes:120}")
    private long expiryMinutes;

    public JwtTokenService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TokenEnvelope issueToken(UUID tenantId, UUID platformUserId, MembershipRole membershipRole, boolean platformAdmin) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiryMinutes, ChronoUnit.MINUTES);

        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", platformUserId.toString());
        payload.put("tenantId", tenantId.toString());
        payload.put("role", membershipRole != null ? membershipRole.name() : null);
        payload.put("platformAdmin", platformAdmin);
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        try {
            String encodedHeader = encodeJson(header);
            String encodedPayload = encodeJson(payload);
            String signature = sign(encodedHeader + "." + encodedPayload);
            return new TokenEnvelope(encodedHeader + "." + encodedPayload + "." + signature, expiresAt);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to issue JWT", ex);
        }
    }

    public JwtClaims verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                return null;
            }

            Map<String, Object> payload = objectMapper.readValue(URL_DECODER.decode(parts[1]), new TypeReference<>() {
            });
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                return null;
            }

            UUID tenantId = UUID.fromString(String.valueOf(payload.get("tenantId")));
            UUID platformUserId = UUID.fromString(String.valueOf(payload.get("sub")));
            String roleValue = payload.get("role") == null ? null : String.valueOf(payload.get("role"));
            MembershipRole role = roleValue == null || roleValue.isBlank() ? null : MembershipRole.valueOf(roleValue);
            boolean platformAdmin = Boolean.parseBoolean(String.valueOf(payload.get("platformAdmin")));
            return new JwtClaims(tenantId, platformUserId, role, platformAdmin, Instant.ofEpochSecond(exp));
        } catch (Exception ex) {
            return null;
        }
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int result = 0;
        for (int index = 0; index < leftBytes.length; index++) {
            result |= leftBytes[index] ^ rightBytes[index];
        }
        return result == 0;
    }

    public record TokenEnvelope(String token, Instant expiresAt) {
    }

    public record JwtClaims(UUID tenantId, UUID platformUserId, MembershipRole membershipRole,
                            boolean platformAdmin, Instant expiresAt) {
    }
}
