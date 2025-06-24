package com.abhishek.notix.common.dto;

import com.abhishek.notix.common.enums.Channel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public class NotificationEvent {

    @NotNull(message = "ID must not be null")
    private UUID id;

    @NotBlank(message = "Recipient (to) is required")
    @Email(message = "Invalid email format")
    private String to;

    @NotNull(message = "Channel is required")
    private Channel channel;

    @NotBlank(message = "Template is required")
    private String template;

    private Map<String, Object> params;

    public NotificationEvent() {}

    public NotificationEvent(UUID id, String to, Channel channel, String template, Map<String, Object> params) {
        this.id = id;
        this.to = to;
        this.channel = channel;
        this.template = template;
        this.params = params;
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
}
