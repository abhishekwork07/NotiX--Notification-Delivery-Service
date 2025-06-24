package com.abhishek.notix.api_service.controller;

import com.abhishek.notix.api_service.service.NotificationService;
import com.abhishek.notix.common.dto.NotificationEvent;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@Valid @RequestBody NotificationEvent event) {
        UUID id = notificationService.processNotification(event);
        return ResponseEntity.ok("Notification queued with ID: " + id);
    }
}

