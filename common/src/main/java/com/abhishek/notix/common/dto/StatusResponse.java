package com.abhishek.notix.common.dto;

import com.abhishek.notix.common.enums.Status;

import java.util.UUID;

public class StatusResponse {

    private UUID id;
    private Status status;

    public StatusResponse() {}

    public StatusResponse(UUID id, Status status) {
        this.id = id;
        this.status = status;
    }

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
