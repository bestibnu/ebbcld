package com.cloudcity.platform.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class DiscoveryStatusResponse {
    private UUID id;
    private String status;
    private Integer progress;
    private OffsetDateTime finishedAt;

    public DiscoveryStatusResponse(UUID id, String status, Integer progress, OffsetDateTime finishedAt) {
        this.id = id;
        this.status = status;
        this.progress = progress;
        this.finishedAt = finishedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public Integer getProgress() {
        return progress;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }
}
