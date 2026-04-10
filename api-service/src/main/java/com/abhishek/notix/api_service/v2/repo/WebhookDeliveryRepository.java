package com.abhishek.notix.api_service.v2.repo;

import com.abhishek.notix.api_service.v2.model.WebhookDelivery;
import com.abhishek.notix.api_service.v2.model.WebhookDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    @Query("""
            SELECT w FROM WebhookDelivery w
            WHERE w.status IN :statuses
              AND (w.nextAttemptAt IS NULL OR w.nextAttemptAt <= :now)
            ORDER BY w.createdAt ASC
            """)
    List<WebhookDelivery> findDispatchable(@Param("statuses") List<WebhookDeliveryStatus> statuses,
                                           @Param("now") Instant now);
}
