package com.abhishek.notix.api_service.controller;

import com.abhishek.notix.api_service.service.NotificationService;
import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.enums.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/test/api")
public class NotificationTestController {

    @Autowired
    private NotificationService notificationService;

    public NotificationTestController(NotificationService notificationService) {
        this.notificationService = notificationService;
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
}
