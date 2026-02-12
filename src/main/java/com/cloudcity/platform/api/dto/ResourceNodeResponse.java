package com.cloudcity.platform.api.dto;

import com.cloudcity.platform.domain.CloudProvider;
import com.cloudcity.platform.domain.ResourceSource;
import com.cloudcity.platform.domain.ResourceType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ResourceNodeResponse {
    private UUID id;
    private UUID projectId;
    private CloudProvider provider;
    private ResourceType type;
    private String name;
    private String region;
    private String zone;
    private String state;
    private ResourceSource source;
    private BigDecimal costEstimate;
    private String metadataJson;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ResourceNodeResponse(UUID id, UUID projectId, CloudProvider provider, ResourceType type, String name, String region,
                                String zone, String state, ResourceSource source, BigDecimal costEstimate,
                                String metadataJson, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.provider = provider;
        this.type = type;
        this.name = name;
        this.region = region;
        this.zone = zone;
        this.state = state;
        this.source = source;
        this.costEstimate = costEstimate;
        this.metadataJson = metadataJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public ResourceType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
    }

    public String getZone() {
        return zone;
    }

    public String getState() {
        return state;
    }

    public ResourceSource getSource() {
        return source;
    }

    public BigDecimal getCostEstimate() {
        return costEstimate;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
