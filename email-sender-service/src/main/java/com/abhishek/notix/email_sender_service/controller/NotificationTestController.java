package com.abhishek.notix.email_sender_service.controller;

import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.enums.Channel;
import com.abhishek.notix.email_sender_service.service.EmailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/test/email")
public class NotificationTestController {

    @Autowired
    private EmailSenderService emailSenderService;

//    public NotificationTestController(EmailSenderService emailSenderService) {
//        this.emailSenderService = emailSenderService;
//    }

    @PostMapping("/send")
    public ResponseEntity<String> sendTestEmail(@RequestBody Map<String, Object> request) {
        try {
            String to = (String) request.get("to");
            String template = (String) request.get("template");
            Map<String, Object> params = (Map<String, Object>) request.get("params");

            // Manually creating NotificationEvent
            NotificationEvent event = new NotificationEvent(
                    UUID.randomUUID(), to, Channel.EMAIL, template, params
            );

            emailSenderService.sendEmail(event); // Direct method call

            return ResponseEntity.ok("Email send flow triggered successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}

