package com.abhishek.notix.common.dto;

import com.abhishek.notix.common.enums.Channel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public class NotificationEvent {

    @NotNull(message = "ID must not be null")
    private UUID id;

    private UUID tenantId;

    @NotBlank(message = "Recipient (to) is required")
//    @Email(message = "Invalid email format")
    private String to;

    @NotNull(message = "Channel is required")
    private Channel channel;

    @NotBlank(message = "Template is required")
    private String template;

    private UUID templateId;

    private Map<String, Object> params;

    private String idempotencyKey;

    private UUID providerAccountId;

    private String subject;

    private String body;

    private Instant scheduledAt;

    @Builder.Default
    @Min(value = 1, message = "Attempt number must be at least 1")
    private int attemptNo = 1;

    public NotificationEvent() {}

    public NotificationEvent(UUID id, String to, Channel channel, String template, Map<String, Object> params) {
        this(id, null, to, channel, template, null, params, null, null, null, null, null, 1);
    }

    public NotificationEvent(UUID id, String to, Channel channel, String template, Map<String, Object> params, int attemptNo) {
        this(id, null, to, channel, template, null, params, null, null, null, null, null, attemptNo);
    }

    public NotificationEvent(UUID id, UUID tenantId, String to, Channel channel, String template, UUID templateId,
                             Map<String, Object> params, String idempotencyKey, UUID providerAccountId,
                             String subject, String body, Instant scheduledAt, int attemptNo) {
        this.id = id;
        this.tenantId = tenantId;
        this.to = to;
        this.channel = channel;
        this.template = template;
        this.templateId = templateId;
        this.params = params;
        this.idempotencyKey = idempotencyKey;
        this.providerAccountId = providerAccountId;
        this.subject = subject;
        this.body = body;
        this.scheduledAt = scheduledAt;
        this.attemptNo = attemptNo;
    }

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public UUID getProviderAccountId() { return providerAccountId; }
    public void setProviderAccountId(UUID providerAccountId) { this.providerAccountId = providerAccountId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }

    public int getAttemptNo() { return attemptNo; }
    public void setAttemptNo(int attemptNo) { this.attemptNo = attemptNo; }
}
