package com.cloudcity.platform.api.dto;

import java.math.BigDecimal;

public class CostPolicyCheckResponse {
    private boolean allowed;
    private BudgetStatus budgetStatus;
    private BigDecimal monthlyBudget;
    private BigDecimal currentTotal;
    private BigDecimal projectedTotal;
    private BigDecimal projectedDelta;
    private BigDecimal budgetUsedPercent;
    private BigDecimal budgetWarningThreshold;
    private String reason;

    public CostPolicyCheckResponse(boolean allowed,
                                   BudgetStatus budgetStatus,
                                   BigDecimal monthlyBudget,
                                   BigDecimal currentTotal,
                                   BigDecimal projectedTotal,
                                   BigDecimal projectedDelta,
                                   BigDecimal budgetUsedPercent,
                                   BigDecimal budgetWarningThreshold,
                                   String reason) {
        this.allowed = allowed;
        this.budgetStatus = budgetStatus;
        this.monthlyBudget = monthlyBudget;
        this.currentTotal = currentTotal;
        this.projectedTotal = projectedTotal;
        this.projectedDelta = projectedDelta;
        this.budgetUsedPercent = budgetUsedPercent;
        this.budgetWarningThreshold = budgetWarningThreshold;
        this.reason = reason;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public BudgetStatus getBudgetStatus() {
        return budgetStatus;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public BigDecimal getCurrentTotal() {
        return currentTotal;
    }

    public BigDecimal getProjectedTotal() {
        return projectedTotal;
    }

    public BigDecimal getProjectedDelta() {
        return projectedDelta;
    }

    public BigDecimal getBudgetUsedPercent() {
        return budgetUsedPercent;
    }

    public BigDecimal getBudgetWarningThreshold() {
        return budgetWarningThreshold;
    }

    public String getReason() {
        return reason;
    }
}
