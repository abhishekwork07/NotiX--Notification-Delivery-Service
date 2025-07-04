package com.abhishek.notix.retry_scheduler_service.repo;

import com.abhishek.notix.retry_scheduler_service.model.DeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeadLetterRepository extends JpaRepository<DeadLetter, UUID> {

    List<DeadLetter> findByChannel(String channel);

    List<DeadLetter> findByTemplate(String template);

    List<DeadLetter> findByChannelAndTemplate(String channel, String template);
}

