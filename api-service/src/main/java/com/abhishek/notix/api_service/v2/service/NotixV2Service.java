package com.abhishek.notix.api_service.v2.service;

import com.abhishek.notix.api_service.model.DeliveryLog;
import com.abhishek.notix.api_service.model.Notification;
import com.abhishek.notix.api_service.repo.DeliveryLogRepository;
import com.abhishek.notix.api_service.repo.NotificationRepository;
import com.abhishek.notix.api_service.v2.dto.*;
import com.abhishek.notix.api_service.v2.model.*;
import com.abhishek.notix.api_service.v2.repo.*;
import com.abhishek.notix.api_service.v2.security.AuthenticatedActor;
import com.abhishek.notix.api_service.v2.security.CredentialHashing;
import com.abhishek.notix.api_service.v2.security.PasswordHashing;
import com.abhishek.notix.api_service.v2.security.TenantRequestContextHolder;
import com.abhishek.notix.common.dto.NotificationEvent;
import com.abhishek.notix.common.dto.NotificationStatusEvent;
import com.abhishek.notix.common.enums.Channel;
import com.abhishek.notix.common.enums.NotificationLifecycleEventType;
import com.abhishek.notix.common.enums.Status;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotixV2Service {

    private static final int WEBHOOK_MAX_ATTEMPTS = 3;

    private final TenantRepository tenantRepository;
    private final PlatformUserRepository platformUserRepository;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ProviderAccountRepository providerAccountRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final UsageEventRepository usageEventRepository;
    private final NotificationScheduleRepository notificationScheduleRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final DeliveryLogRepository deliveryLogRepository;
    private final KafkaTemplate<String, NotificationEvent> notificationKafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${kafka.producer.topic}")
    private String notificationTopic;

    @Value("${notix.security.bootstrap-admin-key}")
    private String bootstrapAdminKey;

    public NotixV2Service(TenantRepository tenantRepository,
                          PlatformUserRepository platformUserRepository,
                          TenantMembershipRepository tenantMembershipRepository,
                          ApiKeyRepository apiKeyRepository,
                          ProviderAccountRepository providerAccountRepository,
                          NotificationTemplateRepository notificationTemplateRepository,
                          UsageEventRepository usageEventRepository,
                          NotificationScheduleRepository notificationScheduleRepository,
                          WebhookEndpointRepository webhookEndpointRepository,
                          WebhookDeliveryRepository webhookDeliveryRepository,
                          AuditLogRepository auditLogRepository,
                          NotificationRepository notificationRepository,
                          DeliveryLogRepository deliveryLogRepository,
                          KafkaTemplate<String, NotificationEvent> notificationKafkaTemplate,
                          ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.platformUserRepository = platformUserRepository;
        this.tenantMembershipRepository = tenantMembershipRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.providerAccountRepository = providerAccountRepository;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.usageEventRepository = usageEventRepository;
        this.notificationScheduleRepository = notificationScheduleRepository;
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.deliveryLogRepository = deliveryLogRepository;
        this.notificationKafkaTemplate = notificationKafkaTemplate;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    @Transactional
    public TenantBootstrapResponse bootstrapTenant(TenantBootstrapRequest request, String providedBootstrapKey) {
        if (!Objects.equals(bootstrapAdminKey, providedBootstrapKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bootstrap admin key");
        }
        tenantRepository.findByDomain(request.domain()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant domain already exists");
        });

        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName(request.name());
        tenant.setDomain(request.domain().toLowerCase(Locale.ROOT));
        tenantRepository.save(tenant);

        String ownerUserKey = normalizeUserKey(request.ownerExternalUserId(), request.ownerEmail());
        PlatformUser owner = platformUserRepository.findByExternalUserId(ownerUserKey)
                .orElseGet(PlatformUser::new);
        if (owner.getId() == null) {
            owner.setId(UUID.randomUUID());
        }
        owner.setExternalUserId(ownerUserKey);
        owner.setEmail(request.ownerEmail());
        owner.setDisplayName(request.ownerDisplayName());
        if (hasText(request.ownerPassword())) {
            owner.setAuthProvider("LOCAL");
            owner.setPasswordHash(PasswordHashing.hashPassword(request.ownerPassword()));
        } else {
            owner.setAuthProvider("EXTERNAL");
            owner.setPasswordHash(null);
        }
        owner.setPlatformAdmin(false);
        owner.setActive(true);
        platformUserRepository.save(owner);

        TenantMembership membership = new TenantMembership();
        membership.setId(UUID.randomUUID());
        membership.setTenantId(tenant.getId());
        membership.setPlatformUserId(owner.getId());
        membership.setRole(MembershipRole.TENANT_ADMIN);
        membership.setActive(true);
        tenantMembershipRepository.save(membership);

        ApiKeyResponse apiKey = createApiKeyInternal(tenant.getId(), "bootstrap-key", owner.getId());

        recordAudit(tenant.getId(), "BOOTSTRAP", "bootstrap-admin",
                "tenant.created", "tenant", tenant.getId().toString(),
                Map.of("domain", tenant.getDomain(), "ownerExternalUserId", owner.getExternalUserId(), "authProvider", owner.getAuthProvider()));

        return new TenantBootstrapResponse(tenant.getId(), owner.getId(), apiKey.id(), apiKey.rawKey());
    }

    @Transactional
    public SimpleCreatedResponse addTenantMembership(TenantMembershipRequest request) {
        AuthenticatedActor actor = requireTenantAdmin();

        String userKey = normalizeUserKey(request.externalUserId(), request.email());
        PlatformUser user = platformUserRepository.findByExternalUserId(userKey)
                .orElseGet(PlatformUser::new);
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        user.setExternalUserId(userKey);
        user.setEmail(request.email());
        user.setDisplayName(request.displayName());
        if (hasText(request.password())) {
            user.setAuthProvider("LOCAL");
            user.setPasswordHash(PasswordHashing.hashPassword(request.password()));
        } else if (user.getAuthProvider() == null) {
            user.setAuthProvider("EXTERNAL");
        }
        user.setActive(true);
        platformUserRepository.save(user);

        if (tenantMembershipRepository.existsByTenantIdAndPlatformUserId(actor.getTenantId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant membership already exists");
        }

        TenantMembership membership = new TenantMembership();
        membership.setId(UUID.randomUUID());
        membership.setTenantId(actor.getTenantId());
        membership.setPlatformUserId(user.getId());
        membership.setRole(request.role());
        membership.setActive(true);
        tenantMembershipRepository.save(membership);

        recordAudit(actor.getTenantId(), actor.actorType(), actor.actorId(),
                "tenant-membership.created", "tenant-membership", membership.getId().toString(),
                Map.of("platformUserId", user.getId(), "role", request.role().name(), "authProvider", user.getAuthProvider()));

        return new SimpleCreatedResponse(membership.getId(), "Tenant membership created");
    }

    @Transactional
    public ApiKeyResponse createApiKey(CreateApiKeyRequest request) {
        AuthenticatedActor actor = requireTenantAdmin();
        return createApiKeyInternal(actor.getTenantId(), request.name(), actor.getPlatformUserId());
    }

    @Transactional
    public SimpleCreatedResponse createProvider(CreateProviderAccountRequest request) {
        AuthenticatedActor actor = requireTenantAdmin();

        ProviderAccount provider = new ProviderAccount();
        provider.setId(UUID.randomUUID());
        provider.setTenantId(actor.getTenantId());
        provider.setName(request.name());
        provider.setChannel(request.channel());
        provider.setProviderType(request.providerType());
        provider.setConfigurationJson(writeJson(request.configuration()));
        provider.setActive(true);
        providerAccountRepository.save(provider);

        recordAudit(actor.getTenantId(), actor.actorType(), actor.actorId(),
                "provider.created", "provider-account", provider.getId().toString(),
                Map.of("channel", request.channel().name(), "providerType", request.providerType().name()));

        return new SimpleCreatedResponse(provider.getId(), "Provider account created");
    }

    @Transactional
    public SimpleCreatedResponse createTemplate(CreateTemplateRequest request) {
        AuthenticatedActor actor = requireTenantAdmin();

        NotificationTemplate template = new NotificationTemplate();
        template.setId(UUID.randomUUID());
        template.setTenantId(actor.getTenantId());
        template.setName(request.name());
        template.setChannel(request.channel());
        template.setSubjectTemplate(request.subjectTemplate());
        template.setBodyTemplate(request.bodyTemplate());
        template.setActive(true);
        notificationTemplateRepository.save(template);

        recordAudit(actor.getTenantId(), actor.actorType(), actor.actorId(),
                "template.created", "template", template.getId().toString(),
                Map.of("channel", request.channel().name(), "name", request.name()));

        return new SimpleCreatedResponse(template.getId(), "Template created");
    }

    @Transactional
    public SimpleCreatedResponse createWebhook(CreateWebhookEndpointRequest request) {
        AuthenticatedActor actor = requireTenantAdmin();

        WebhookEndpoint endpoint = new WebhookEndpoint();
        endpoint.setId(UUID.randomUUID());
        endpoint.setTenantId(actor.getTenantId());
        endpoint.setName(request.name());
        endpoint.setUrl(request.url());
        endpoint.setSecret(generateSecret());
        endpoint.setSubscribedEvents(request.subscribedEvents().stream().map(Enum::name).collect(Collectors.joining(",")));
        endpoint.setActive(true);
        webhookEndpointRepository.save(endpoint);

        recordAudit(actor.getTenantId(), actor.actorType(), actor.actorId(),
                "webhook.created", "webhook-endpoint", endpoint.getId().toString(),
                Map.of("url", request.url(), "subscribedEvents", endpoint.getSubscribedEvents()));

        return new SimpleCreatedResponse(endpoint.getId(), "Webhook endpoint created");
    }

    @Transactional
    public NotificationV2Response createNotification(SendNotificationV2Request request) {
        return createNotificationInternal(request, false);
    }

    @Transactional
    public NotificationV2Response createSchedule(SendNotificationV2Request request) {
        if (request.scheduledAt() == null || !request.scheduledAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scheduledAt must be a future timestamp");
        }
        return createNotificationInternal(request, true);
    }

    @Transactional(readOnly = true)
    public NotificationV2Response getNotification(UUID id) {
        AuthenticatedActor actor = TenantRequestContextHolder.require();
        Notification notification = notificationRepository.findByIdAndTenantId(id, actor.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        return toResponse(notification);
    }

    @Transactional(readOnly = true)
    public List<DeliveryAttemptResponse> getAttempts(UUID id) {
        AuthenticatedActor actor = TenantRequestContextHolder.require();
        notificationRepository.findByIdAndTenantId(id, actor.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        return deliveryLogRepository.findByNotificationIdAndTenantIdOrderByAttemptNoAscTimestampAsc(id, actor.getTenantId())
                .stream()
                .map(this::toAttemptResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsageSummaryResponse getUsage(Instant from, Instant to) {
        AuthenticatedActor actor = TenantRequestContextHolder.require();
        Instant fromTs = from == null ? Instant.now().minus(30, ChronoUnit.DAYS) : from;
        Instant toTs = to == null ? Instant.now() : to;

        Map<String, Long> totals = usageEventRepository.findByTenantIdAndCreatedAtBetween(actor.getTenantId(), fromTs, toTs)
                .stream()
                .collect(Collectors.groupingBy(event -> event.getEventType().name(), TreeMap::new, Collectors.counting()));

        return new UsageSummaryResponse(actor.getTenantId(), fromTs, toTs, totals);
    }

    @Transactional
    public void handleStatusEvent(NotificationStatusEvent event) {
        if (event.getTenantId() == null) {
            return;
        }
        recordUsageEvent(event.getTenantId(), event.getNotificationId(), event.getChannel(), mapUsageEvent(event.getEventType()),
                Map.of("attemptNo", event.getAttemptNo(), "status", event.getStatus().name(), "errorMessage", defaultString(event.getErrorMessage())));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", event.getTenantId());
        payload.put("notificationId", event.getNotificationId());
        payload.put("channel", event.getChannel());
        payload.put("status", event.getStatus());
        payload.put("eventType", event.getEventType());
        payload.put("attemptNo", event.getAttemptNo());
        payload.put("errorMessage", defaultString(event.getErrorMessage()));
        payload.put("occurredAt", event.getOccurredAt());
        createWebhookDeliveries(event.getTenantId(), event.getNotificationId(), event.getEventType(), payload);
    }

    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void dispatchDueSchedules() {
        List<NotificationSchedule> dueSchedules = notificationScheduleRepository.findByStatusAndScheduledAtBefore(ScheduleStatus.PENDING, Instant.now());
        for (NotificationSchedule schedule : dueSchedules) {
            try {
                Notification notification = notificationRepository.findByIdAndTenantId(schedule.getNotificationId(), schedule.getTenantId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scheduled notification not found"));
                notification.setStatus(Status.PENDING);
                notificationRepository.save(notification);
                publishNotification(notification);
                schedule.setStatus(ScheduleStatus.DISPATCHED);
                schedule.setDispatchedAt(Instant.now());
                notificationScheduleRepository.save(schedule);
            } catch (Exception ex) {
                schedule.setStatus(ScheduleStatus.FAILED);
                schedule.setLastError(ex.getMessage());
                notificationScheduleRepository.save(schedule);
            }
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 10000)
    public void dispatchWebhookDeliveries() {
        List<WebhookDelivery> deliveries = webhookDeliveryRepository.findDispatchable(
                List.of(WebhookDeliveryStatus.PENDING, WebhookDeliveryStatus.FAILED),
                Instant.now()
        );

        for (WebhookDelivery delivery : deliveries) {
            WebhookEndpoint endpoint = webhookEndpointRepository.findById(delivery.getEndpointId()).orElse(null);
            if (endpoint == null || !endpoint.isActive()) {
                delivery.setStatus(WebhookDeliveryStatus.DEAD_LETTERED);
                delivery.setLastError("Webhook endpoint is missing or inactive");
                webhookDeliveryRepository.save(delivery);
                continue;
            }

            try {
                restClient.post()
                        .uri(endpoint.getUrl())
                        .header("Content-Type", "application/json")
                        .header("X-Notix-Signature", hmacSha256(delivery.getPayloadJson(), endpoint.getSecret()))
                        .body(delivery.getPayloadJson())
                        .retrieve()
                        .toBodilessEntity();

                delivery.setStatus(WebhookDeliveryStatus.DELIVERED);
                delivery.setAttempts(delivery.getAttempts() + 1);
                delivery.setDeliveredAt(Instant.now());
                delivery.setLastError(null);
                webhookDeliveryRepository.save(delivery);

                recordUsageEvent(delivery.getTenantId(), delivery.getNotificationId(), null,
                        UsageEventType.WEBHOOK_DELIVERED, Map.of("endpointId", endpoint.getId()));
            } catch (Exception ex) {
                int nextAttempt = delivery.getAttempts() + 1;
                delivery.setAttempts(nextAttempt);
                delivery.setLastError(ex.getMessage());
                if (nextAttempt >= WEBHOOK_MAX_ATTEMPTS) {
                    delivery.setStatus(WebhookDeliveryStatus.DEAD_LETTERED);
                } else {
                    delivery.setStatus(WebhookDeliveryStatus.FAILED);
                    delivery.setNextAttemptAt(Instant.now().plus(1, ChronoUnit.MINUTES));
                }
                webhookDeliveryRepository.save(delivery);

                recordUsageEvent(delivery.getTenantId(), delivery.getNotificationId(), null,
                        UsageEventType.WEBHOOK_FAILED, Map.of("endpointId", endpoint.getId(), "errorMessage", defaultString(ex.getMessage())));
            }
        }
    }

    private NotificationV2Response createNotificationInternal(SendNotificationV2Request request, boolean forceScheduled) {
        AuthenticatedActor actor = TenantRequestContextHolder.require();

        String idempotencyKey = request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                ? UUID.randomUUID().toString()
                : request.idempotencyKey();

        Optional<Notification> existing = notificationRepository.findByTenantIdAndIdempotencyKey(actor.getTenantId(), idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        NotificationTemplate template = null;
        if (request.templateId() != null) {
            template = notificationTemplateRepository.findByIdAndTenantId(request.templateId(), actor.getTenantId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
        }

        ProviderAccount provider = resolveProvider(actor.getTenantId(), request.channel(), request.providerAccountId());
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setTenantId(actor.getTenantId());
        notification.setRecipient(request.to());
        notification.setChannel(request.channel());
        notification.setTemplate(template != null ? template.getName() : "INLINE");
        notification.setTemplateId(template != null ? template.getId() : null);
        notification.setProviderAccountId(provider.getId());
        notification.setIdempotencyKey(idempotencyKey);
        notification.setRequestedByType(actor.actorType());
        notification.setRequestedById(actor.actorId());
        notification.setSubject(resolveSubject(request, template));
        notification.setBody(resolveBody(request, template));
        notification.setScheduledAt(request.scheduledAt());
        notification.setStatus(forceScheduled || isFutureSchedule(request.scheduledAt()) ? Status.SCHEDULED : Status.PENDING);
        notificationRepository.save(notification);

        if (notification.getStatus() == Status.SCHEDULED) {
            NotificationSchedule schedule = new NotificationSchedule();
            schedule.setId(UUID.randomUUID());
            schedule.setTenantId(actor.getTenantId());
            schedule.setNotificationId(notification.getId());
            schedule.setScheduledAt(request.scheduledAt());
            schedule.setStatus(ScheduleStatus.PENDING);
            notificationScheduleRepository.save(schedule);
        } else {
            publishNotification(notification);
        }

        recordUsageEvent(notification.getTenantId(), notification.getId(), notification.getChannel(), UsageEventType.ACCEPTED,
                Map.of("scheduled", notification.getStatus() == Status.SCHEDULED));
        Map<String, Object> acceptedPayload = new LinkedHashMap<>();
        acceptedPayload.put("tenantId", notification.getTenantId());
        acceptedPayload.put("notificationId", notification.getId());
        acceptedPayload.put("channel", notification.getChannel());
        acceptedPayload.put("status", notification.getStatus());
        acceptedPayload.put("recipient", notification.getRecipient());
        acceptedPayload.put("scheduledAt", notification.getScheduledAt());
        acceptedPayload.put("acceptedAt", Instant.now());
        createWebhookDeliveries(notification.getTenantId(), notification.getId(), NotificationLifecycleEventType.ACCEPTED, acceptedPayload);
        recordAudit(actor.getTenantId(), actor.actorType(), actor.actorId(),
                notification.getStatus() == Status.SCHEDULED ? "notification.scheduled" : "notification.accepted",
                "notification", notification.getId().toString(),
                Map.of("channel", notification.getChannel().name(), "template", notification.getTemplate()));

        return toResponse(notification);
    }

    private ApiKeyResponse createApiKeyInternal(UUID tenantId, String name, UUID createdByUserId) {
        String rawKey = "notix_" + UUID.randomUUID().toString().replace("-", "") + randomSuffix();
        ApiKey apiKey = new ApiKey();
        apiKey.setId(UUID.randomUUID());
        apiKey.setTenantId(tenantId);
        apiKey.setName(name);
        apiKey.setKeyPrefix(rawKey.substring(0, Math.min(12, rawKey.length())));
        apiKey.setHashedKey(CredentialHashing.sha256(rawKey));
        apiKey.setCreatedByUserId(createdByUserId);
        apiKey.setActive(true);
        apiKeyRepository.save(apiKey);

        recordAudit(tenantId, createdByUserId != null ? "USER" : "BOOTSTRAP",
                createdByUserId != null ? createdByUserId.toString() : "bootstrap-admin",
                "api-key.created", "api-key", apiKey.getId().toString(),
                Map.of("name", name, "keyPrefix", apiKey.getKeyPrefix()));

        return new ApiKeyResponse(apiKey.getId(), apiKey.getName(), apiKey.getKeyPrefix(), rawKey, apiKey.getCreatedAt());
    }

    private ProviderAccount resolveProvider(UUID tenantId, Channel channel, UUID requestedProviderId) {
        if (requestedProviderId != null) {
            return providerAccountRepository.findByIdAndTenantId(requestedProviderId, tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider account not found"));
        }
        return providerAccountRepository.findFirstByTenantIdAndChannelAndActiveTrueOrderByCreatedAtAsc(tenantId, channel)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active provider configured for channel " + channel));
    }

    private void publishNotification(Notification notification) {
        NotificationEvent event = NotificationEvent.builder()
                .id(notification.getId())
                .tenantId(notification.getTenantId())
                .to(notification.getRecipient())
                .channel(notification.getChannel())
                .template(notification.getTemplate())
                .templateId(notification.getTemplateId())
                .idempotencyKey(notification.getIdempotencyKey())
                .providerAccountId(notification.getProviderAccountId())
                .subject(notification.getSubject())
                .body(notification.getBody())
                .scheduledAt(notification.getScheduledAt())
                .attemptNo(1)
                .build();
        notificationKafkaTemplate.send(notificationTopic, notification.getId().toString(), event);
    }

    private String resolveSubject(SendNotificationV2Request request, NotificationTemplate template) {
        if (template != null) {
            return renderTemplate(template.getSubjectTemplate(), request.params());
        }
        return request.subject();
    }

    private String resolveBody(SendNotificationV2Request request, NotificationTemplate template) {
        if (template != null) {
            return renderTemplate(template.getBodyTemplate(), request.params());
        }
        if (request.body() == null || request.body().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required when templateId is not provided");
        }
        return request.body();
    }

    private String renderTemplate(String template, Map<String, Object> params) {
        if (template == null) {
            return null;
        }
        String rendered = template;
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
        }
        return rendered;
    }

    private boolean isFutureSchedule(Instant scheduledAt) {
        return scheduledAt != null && scheduledAt.isAfter(Instant.now());
    }

    private NotificationV2Response toResponse(Notification notification) {
        List<DeliveryAttemptResponse> attempts = notification.getTenantId() == null
                ? deliveryLogRepository.findByNotificationIdOrderByAttemptNoAscTimestampAsc(notification.getId()).stream().map(this::toAttemptResponse).toList()
                : deliveryLogRepository.findByNotificationIdAndTenantIdOrderByAttemptNoAscTimestampAsc(notification.getId(), notification.getTenantId())
                .stream()
                .map(this::toAttemptResponse)
                .toList();

        return new NotificationV2Response(
                notification.getId(),
                notification.getTenantId(),
                notification.getRecipient(),
                notification.getChannel(),
                notification.getStatus(),
                notification.getTemplate(),
                notification.getTemplateId(),
                notification.getProviderAccountId(),
                notification.getSubject(),
                notification.getBody(),
                notification.getIdempotencyKey(),
                notification.getScheduledAt(),
                attempts
        );
    }

    private DeliveryAttemptResponse toAttemptResponse(DeliveryLog log) {
        return new DeliveryAttemptResponse(
                log.getAttemptNo(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getTimestamp(),
                log.getChannel(),
                log.getProviderAccountId()
        );
    }

    private UsageEventType mapUsageEvent(NotificationLifecycleEventType eventType) {
        return switch (eventType) {
            case ACCEPTED -> UsageEventType.ACCEPTED;
            case SENT -> UsageEventType.SENT;
            case FAILED -> UsageEventType.FAILED;
            case DEAD_LETTERED -> UsageEventType.DEAD_LETTERED;
        };
    }

    private void recordUsageEvent(UUID tenantId, UUID notificationId, Channel channel, UsageEventType eventType, Map<String, Object> metadata) {
        UsageEvent event = new UsageEvent();
        event.setId(UUID.randomUUID());
        event.setTenantId(tenantId);
        event.setNotificationId(notificationId);
        event.setChannel(channel);
        event.setEventType(eventType);
        event.setQuantity(1);
        event.setMetadataJson(writeJson(metadata));
        usageEventRepository.save(event);
    }

    private void createWebhookDeliveries(UUID tenantId, UUID notificationId, NotificationLifecycleEventType eventType, Map<String, Object> payload) {
        String payloadJson = writeJson(payload);
        for (WebhookEndpoint endpoint : webhookEndpointRepository.findByTenantIdAndActiveTrue(tenantId)) {
            Set<String> subscribedEvents = Arrays.stream(endpoint.getSubscribedEvents().split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .collect(Collectors.toSet());
            if (!subscribedEvents.contains(eventType.name())) {
                continue;
            }

            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setId(UUID.randomUUID());
            delivery.setTenantId(tenantId);
            delivery.setEndpointId(endpoint.getId());
            delivery.setNotificationId(notificationId);
            delivery.setEventType(eventType);
            delivery.setPayloadJson(payloadJson);
            delivery.setStatus(WebhookDeliveryStatus.PENDING);
            delivery.setAttempts(0);
            delivery.setNextAttemptAt(Instant.now());
            webhookDeliveryRepository.save(delivery);
        }
    }

    private void recordAudit(UUID tenantId, String actorType, String actorId, String action,
                             String resourceType, String resourceId, Map<String, Object> metadata) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(UUID.randomUUID());
        auditLog.setTenantId(tenantId);
        auditLog.setActorType(actorType);
        auditLog.setActorId(actorId);
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setMetadataJson(writeJson(metadata));
        auditLogRepository.save(auditLog);
    }

    private AuthenticatedActor requireTenantAdmin() {
        AuthenticatedActor actor = TenantRequestContextHolder.require();
        if (!actor.isTenantAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant admin access is required");
        }
        return actor;
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize JSON payload", ex);
        }
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String normalizeUserKey(String candidate, String email) {
        return hasText(candidate) ? candidate.trim() : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String generateSecret() {
        return UUID.randomUUID() + randomSuffix();
    }

    private String randomSuffix() {
        byte[] bytes = new byte[6];
        new SecureRandom().nextBytes(bytes);
        StringBuilder builder = new StringBuilder();
        for (byte value : bytes) {
            builder.append(Integer.toHexString(value & 0xff));
        }
        return builder.toString();
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create webhook signature", ex);
        }
    }
}
