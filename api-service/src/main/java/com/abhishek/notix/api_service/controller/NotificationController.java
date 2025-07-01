package com.abhishek.notix.api_service.controller;

import com.abhishek.notix.api_service.dto.StatusResponseDTO;
import com.abhishek.notix.api_service.exception.NotFoundException;
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

    @GetMapping("/status/{id}")
    @Operation(
            summary = "Get notification status",
            description = "Returns the delivery status and attempts of the notification by ID"
    )
    @ApiResponse(responseCode = "200", description = "Returns status and attempts")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    public ResponseEntity<StatusResponseDTO> getStatus(@PathVariable UUID id) {
        return notificationService.getStatus(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Notification ID not found: " + id));
    }


}

