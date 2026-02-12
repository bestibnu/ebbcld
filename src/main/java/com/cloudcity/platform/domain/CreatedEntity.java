package com.cloudcity.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@MappedSuperclass
public abstract class CreatedEntity {
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    protected void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
