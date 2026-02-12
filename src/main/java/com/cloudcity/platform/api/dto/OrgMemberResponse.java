package com.cloudcity.platform.api.dto;

import com.cloudcity.platform.domain.OrgRole;
import java.time.OffsetDateTime;
import java.util.UUID;

public class OrgMemberResponse {
    private UUID orgId;
    private UUID userId;
    private OrgRole role;
    private OffsetDateTime createdAt;

    public OrgMemberResponse(UUID orgId, UUID userId, OrgRole role, OffsetDateTime createdAt) {
        this.orgId = orgId;
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public UUID getUserId() {
        return userId;
    }

    public OrgRole getRole() {
        return role;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
