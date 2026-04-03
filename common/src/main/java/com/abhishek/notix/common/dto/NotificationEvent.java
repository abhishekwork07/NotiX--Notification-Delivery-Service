package com.abhishek.notix.common.dto;

import com.abhishek.notix.common.enums.Channel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.Map;
import java.util.UUID;

@Builder
public class NotificationEvent {

    @NotNull(message = "ID must not be null")
    private UUID id;

    @NotBlank(message = "Recipient (to) is required")
//    @Email(message = "Invalid email format")
    private String to;

    @NotNull(message = "Channel is required")
    private Channel channel;

    @NotBlank(message = "Template is required")
    private String template;

    private Map<String, Object> params;

    @Builder.Default
    @Min(value = 1, message = "Attempt number must be at least 1")
    private int attemptNo = 1;

    public NotificationEvent() {}

    public NotificationEvent(UUID id, String to, Channel channel, String template, Map<String, Object> params) {
        this(id, to, channel, template, params, 1);
    }

    public NotificationEvent(UUID id, String to, Channel channel, String template, Map<String, Object> params, int attemptNo) {
        this.id = id;
        this.to = to;
        this.channel = channel;
        this.template = template;
        this.params = params;
        this.attemptNo = attemptNo;
    }

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    public int getAttemptNo() { return attemptNo; }
    public void setAttemptNo(int attemptNo) { this.attemptNo = attemptNo; }
}
