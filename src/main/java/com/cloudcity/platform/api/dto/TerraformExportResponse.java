package com.cloudcity.platform.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TerraformExportResponse {
    private UUID id;
    private UUID projectId;
    private String status;
    private String summaryJson;
    private String artifactPath;
    private OffsetDateTime createdAt;

    public TerraformExportResponse(UUID id, UUID projectId, String status, String summaryJson,
                                   String artifactPath, OffsetDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.status = status;
        this.summaryJson = summaryJson;
        this.artifactPath = artifactPath;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getStatus() {
        return status;
    }

    public String getSummaryJson() {
        return summaryJson;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
