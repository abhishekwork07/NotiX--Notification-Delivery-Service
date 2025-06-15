package com.abhishek.notix.dispatcher_service.dto;

import com.abhishek.notix.dispatcher_service.enums.Channel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class SendRequest {

    @NotBlank
    @Email
    private String to;

    @NotNull
    private Channel channel;

    @NotBlank
    private String template;

    private Map<String, Object> params;

    public SendRequest() {
        // Default constructor
    }

    // Getters and Setters

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
}
