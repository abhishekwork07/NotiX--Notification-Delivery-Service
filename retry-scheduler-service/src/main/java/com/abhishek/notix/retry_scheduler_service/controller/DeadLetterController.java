package com.abhishek.notix.retry_scheduler_service.controller;

import com.abhishek.notix.retry_scheduler_service.model.DeadLetter;
import com.abhishek.notix.retry_scheduler_service.repo.DeadLetterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dlq")
public class DeadLetterController {

    @Autowired
    private DeadLetterRepository deadLetterRepository;

    // 🔍 View All Dead Letters
    @GetMapping
    public List<DeadLetter> getAll() {
        return deadLetterRepository.findAll();
    }

    // 🔍 Filter by channel
    @GetMapping("/channel/{channel}")
    public List<DeadLetter> getByChannel(@PathVariable String channel) {
        return deadLetterRepository.findByChannel(channel.toUpperCase());
    }

    // 🔍 Filter by template
    @GetMapping("/template/{template}")
    public List<DeadLetter> getByTemplate(@PathVariable String template) {
        return deadLetterRepository.findByTemplate(template);
    }

    // 🔍 Filter by both
    @GetMapping("/search")
    public List<DeadLetter> getByChannelAndTemplate(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String template
    ) {
        if (channel != null && template != null) {
            return deadLetterRepository.findByChannelAndTemplate(channel.toUpperCase(), template);
        } else if (channel != null) {
            return deadLetterRepository.findByChannel(channel.toUpperCase());
        } else if (template != null) {
            return deadLetterRepository.findByTemplate(template);
        } else {
            return deadLetterRepository.findAll();
        }
    }
}
