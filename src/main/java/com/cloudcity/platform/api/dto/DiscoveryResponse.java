package com.cloudcity.platform.api.dto;

import com.cloudcity.platform.domain.CloudProvider;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DiscoveryResponse {
    private UUID id;
    private UUID projectId;
    private CloudProvider provider;
    private String status;
    private String accountId;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private String summaryJson;

    public DiscoveryResponse(UUID id, UUID projectId, CloudProvider provider, String status, String accountId,
                             OffsetDateTime startedAt, OffsetDateTime finishedAt, String summaryJson) {
        this.id = id;
        this.projectId = projectId;
        this.provider = provider;
        this.status = status;
        this.accountId = accountId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.summaryJson = summaryJson;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public CloudProvider getProvider() {
        return provider;
    }

    public String getStatus() {
        return status;
    }

    public String getAccountId() {
        return accountId;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public String getSummaryJson() {
        return summaryJson;
    }
}
