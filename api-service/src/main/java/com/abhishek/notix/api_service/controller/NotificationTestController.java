package com.abhishek.notix.api_service.controller;

import com.abhishek.notix.api_service.service.NotificationService;
import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.enums.Channel;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/test/api")
public class NotificationTestController {

    private NotificationService notificationService;

    // Simple in-memory bucket (shared across all users for demo)
    private final Bucket bucket;

    public NotificationTestController(NotificationService notificationService) {
        this.notificationService = notificationService;
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofSeconds(10)));
        this.bucket = Bucket.builder().addLimit(limit).build();
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendTestNotification(@RequestBody Map<String, Object> request) {
        try {
            String to = (String) request.get("to");
            String template = (String) request.get("template");
            Map<String, Object> params = (Map<String, Object>) request.get("params");

            Channel channel = Channel.valueOf(((String) request.get("channel")).toUpperCase());

            NotificationEvent event = new NotificationEvent(
                    UUID.randomUUID(), to, channel, template, params
            );

            UUID id = notificationService.processNotification(event);

            return ResponseEntity.ok("Test notification queued successfully with ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/rate-limiter")
    public ResponseEntity<String> testRateLimiter(
            @RequestHeader(value = "X-API-KEY", required = true) String apiKey) {
        final String VALID_API_KEY = "notix-secure-key";

        if (!VALID_API_KEY.equals(apiKey)) {
            return ResponseEntity.status(401).body("  ❌ Unauthorized: Invalid API Key");
        }
        if (bucket.tryConsume(1)) {
            return ResponseEntity.ok("  ✅ Request successful!");
        } else {
            return ResponseEntity.status(429).body("  ❌ Too Many Requests. Try again later.");
        }
    }

}
