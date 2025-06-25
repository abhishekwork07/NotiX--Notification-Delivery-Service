package com.abhishek.notix.api_service.controller;

import com.abhishek.notix.api_service.service.NotificationService;
import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.dto.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(
            summary = "Send a notification",
            description = "Publishes a notification event to Kafka for asynchronous processing"
    )
    @ApiResponse(responseCode = "200", description = "Notification successfully queued")
    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@Valid @RequestBody NotificationEvent event) {
        UUID id = notificationService.processNotification(event);
        return ResponseEntity.ok("Notification queued with ID: " + id);
    }

    @Operation(
            summary = "Get notification status",
            description = "Returns the current delivery status of the notification by ID"
    )
    @ApiResponse(responseCode = "200", description = "Returns status like PENDING, SENT, or FAILED")
    @GetMapping("/status/{id}")
    public ResponseEntity<StatusResponse> getStatus(@PathVariable UUID id) {
        StatusResponse resp = notificationService.getStatus(id);
        return ResponseEntity.ok(resp);
    }

}

