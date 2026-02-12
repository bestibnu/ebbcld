package com.cloudcity.platform.api.dto;

import com.cloudcity.platform.domain.RelationType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ResourceEdgeRequest {
    @NotNull
    private UUID fromNodeId;

    @NotNull
    private UUID toNodeId;

    @NotNull
    private RelationType relationType;

    public UUID getFromNodeId() {
        return fromNodeId;
    }

    public void setFromNodeId(UUID fromNodeId) {
        this.fromNodeId = fromNodeId;
    }

    public UUID getToNodeId() {
        return toNodeId;
    }

    public void setToNodeId(UUID toNodeId) {
        this.toNodeId = toNodeId;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public void setRelationType(RelationType relationType) {
        this.relationType = relationType;
    }
}
