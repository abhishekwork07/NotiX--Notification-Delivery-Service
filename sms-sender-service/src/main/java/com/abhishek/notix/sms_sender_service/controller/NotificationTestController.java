package com.abhishek.notix.sms_sender_service.controller;

import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.enums.Channel;
import com.abhishek.notix.sms_sender_service.service.SmsSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/test/sms")
public class NotificationTestController {

    @Autowired
    private SmsSenderService smsSenderService;

    @PostMapping("/send")
    public ResponseEntity<String> sendTestSms(@RequestBody Map<String, Object> request) {
        try {
            String to = (String) request.get("to");
            String template = (String) request.get("template");
            Map<String, Object> params = (Map<String, Object>) request.get("params");

            NotificationEvent event = new NotificationEvent(
                    UUID.randomUUID(), to, Channel.SMS, template, params
            );

            smsSenderService.sendSms(event); // Direct method call (bypassing Kafka)

            return ResponseEntity.ok("SMS send flow triggered successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
