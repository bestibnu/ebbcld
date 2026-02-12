package com.cloudcity.platform.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public class ProjectUpdateRequest {
    private String name;
    private String description;

    @DecimalMin(value = "0.0")
    private BigDecimal monthlyBudget;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal budgetWarningThreshold;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public BigDecimal getBudgetWarningThreshold() {
        return budgetWarningThreshold;
    }

    public void setBudgetWarningThreshold(BigDecimal budgetWarningThreshold) {
        this.budgetWarningThreshold = budgetWarningThreshold;
    }
}
