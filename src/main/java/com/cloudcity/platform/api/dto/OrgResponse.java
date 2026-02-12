package com.cloudcity.platform.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class OrgResponse {
    private UUID id;
    private String name;
    private OffsetDateTime createdAt;

    public OrgResponse(UUID id, String name, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
