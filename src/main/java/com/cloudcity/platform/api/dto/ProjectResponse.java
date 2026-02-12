package com.cloudcity.platform.api.dto;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.UUID;

public class ProjectResponse {
    private UUID id;
    private UUID orgId;
    private String name;
    private String description;
    private BigDecimal monthlyBudget;
    private BigDecimal budgetWarningThreshold;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ProjectResponse(UUID id,
                           UUID orgId,
                           String name,
                           String description,
                           BigDecimal monthlyBudget,
                           BigDecimal budgetWarningThreshold,
                           OffsetDateTime createdAt,
                           OffsetDateTime updatedAt) {
        this.id = id;
        this.orgId = orgId;
        this.name = name;
        this.description = description;
        this.monthlyBudget = monthlyBudget;
        this.budgetWarningThreshold = budgetWarningThreshold;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public BigDecimal getBudgetWarningThreshold() {
        return budgetWarningThreshold;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
