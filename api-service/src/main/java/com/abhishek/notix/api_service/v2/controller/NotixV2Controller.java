package com.abhishek.notix.api_service.v2.controller;

import com.abhishek.notix.api_service.v2.dto.*;
import com.abhishek.notix.api_service.v2.service.NotixV2Service;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v2")
public class NotixV2Controller {

    private final NotixV2Service notixV2Service;

    public NotixV2Controller(NotixV2Service notixV2Service) {
        this.notixV2Service = notixV2Service;
    }

    @PostMapping("/tenants")
    public TenantBootstrapResponse createTenant(@Valid @RequestBody TenantBootstrapRequest request,
                                                @RequestHeader("X-NOTIX-BOOTSTRAP-KEY") String bootstrapKey) {
        return notixV2Service.bootstrapTenant(request, bootstrapKey);
    }

    @PostMapping("/tenant-memberships")
    public SimpleCreatedResponse createTenantMembership(@Valid @RequestBody TenantMembershipRequest request) {
        return notixV2Service.addTenantMembership(request);
    }

    @PostMapping("/api-keys")
    public ApiKeyResponse createApiKey(@Valid @RequestBody CreateApiKeyRequest request) {
        return notixV2Service.createApiKey(request);
    }

    @PostMapping("/providers")
    public SimpleCreatedResponse createProvider(@Valid @RequestBody CreateProviderAccountRequest request) {
        return notixV2Service.createProvider(request);
    }

    @PostMapping("/templates")
    public SimpleCreatedResponse createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        return notixV2Service.createTemplate(request);
    }

    @PostMapping("/webhooks")
    public SimpleCreatedResponse createWebhook(@Valid @RequestBody CreateWebhookEndpointRequest request) {
        return notixV2Service.createWebhook(request);
    }

    @PostMapping("/notifications")
    public NotificationV2Response sendNotification(@Valid @RequestBody SendNotificationV2Request request) {
        return notixV2Service.createNotification(request);
    }

    @GetMapping("/notifications/{id}")
    public NotificationV2Response getNotification(@PathVariable UUID id) {
        return notixV2Service.getNotification(id);
    }

    @GetMapping("/notifications/{id}/attempts")
    public List<DeliveryAttemptResponse> getAttempts(@PathVariable UUID id) {
        return notixV2Service.getAttempts(id);
    }

    @PostMapping("/schedules")
    public NotificationV2Response createSchedule(@Valid @RequestBody SendNotificationV2Request request) {
        return notixV2Service.createSchedule(request);
    }

    @GetMapping("/usage")
    public UsageSummaryResponse getUsage(@RequestParam(required = false) Instant from,
                                         @RequestParam(required = false) Instant to) {
        return notixV2Service.getUsage(from, to);
    }
}
