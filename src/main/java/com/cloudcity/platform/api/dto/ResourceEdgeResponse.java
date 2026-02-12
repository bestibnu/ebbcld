package com.cloudcity.platform.api.dto;

import com.cloudcity.platform.domain.RelationType;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ResourceEdgeResponse {
    private UUID id;
    private UUID projectId;
    private UUID fromNodeId;
    private UUID toNodeId;
    private RelationType relationType;
    private OffsetDateTime createdAt;

    public ResourceEdgeResponse(UUID id, UUID projectId, UUID fromNodeId, UUID toNodeId, RelationType relationType,
                                OffsetDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.relationType = relationType;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getFromNodeId() {
        return fromNodeId;
    }

    public UUID getToNodeId() {
        return toNodeId;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
