package com.abhishek.notix.retry_scheduler_service.controller;

import com.abhishek.notix.retry_scheduler_service.model.DeadLetter;
import com.abhishek.notix.retry_scheduler_service.service.RetrySchedulerService;
import com.abhishek.notix.retry_scheduler_service.repo.DeadLetterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/retry")
public class RetryController {

    @Autowired
    private RetrySchedulerService retrySchedulerService;

    @Autowired
    private DeadLetterRepository deadLetterRepository;

    /**
     * Manually trigger the retry logic for failed messages
     */
    @PostMapping("/trigger")
    public ResponseEntity<String> triggerRetry() {
        try {
            retrySchedulerService.retryFailedMessages();
            return ResponseEntity.ok("✅ Retry process executed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("❌ Error executing retry: " + e.getMessage());
        }
    }

    /**
     * List all dead-letter entries
     */
    @GetMapping("/dead-letters")
    public ResponseEntity<List<DeadLetter>> listDeadLetters() {
        List<DeadLetter> deadLetters = deadLetterRepository.findAll();
        return ResponseEntity.ok(deadLetters);
    }
}
