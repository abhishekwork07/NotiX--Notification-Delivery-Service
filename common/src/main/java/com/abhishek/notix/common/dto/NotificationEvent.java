package com.abhishek.notix.common.dto;

import com.abhishek.notix.common.enums.Channel;

import java.util.Map;
import java.util.UUID;

public class NotificationEvent {

    private UUID id;
    private String to;
    private Channel channel;
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
