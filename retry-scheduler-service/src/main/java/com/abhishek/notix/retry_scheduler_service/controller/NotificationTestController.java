package com.abhishek.notix.retry_scheduler_service.controller;

import com.abhishek.notix.retry_scheduler_service.service.RetrySchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/retry")
public class NotificationTestController {

    @Autowired
    private RetrySchedulerService retrySchedulerService;

    public NotificationTestController(RetrySchedulerService retrySchedulerService) {
        this.retrySchedulerService = retrySchedulerService;
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerManualRetry() {
        try {
            retrySchedulerService.retryFailedMessages();
            return ResponseEntity.ok("Manual retry process executed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error during retry: " + e.getMessage());
        }
    }
}

