package com.cloudcity.platform.api.dto;

import java.math.BigDecimal;

public class CostDeltaResponse {
    private BigDecimal previousTotal;
    private BigDecimal currentTotal;
    private BigDecimal delta;
    private String currency;
    private BudgetStatus budgetStatus;
    private BigDecimal monthlyBudget;
    private BigDecimal budgetUsedPercent;
    private BigDecimal budgetWarningThreshold;

    public CostDeltaResponse(BigDecimal previousTotal,
                             BigDecimal currentTotal,
                             BigDecimal delta,
                             String currency,
                             BudgetStatus budgetStatus,
                             BigDecimal monthlyBudget,
                             BigDecimal budgetUsedPercent,
                             BigDecimal budgetWarningThreshold) {
        this.previousTotal = previousTotal;
        this.currentTotal = currentTotal;
        this.delta = delta;
        this.currency = currency;
        this.budgetStatus = budgetStatus;
        this.monthlyBudget = monthlyBudget;
        this.budgetUsedPercent = budgetUsedPercent;
        this.budgetWarningThreshold = budgetWarningThreshold;
    }

    public BigDecimal getPreviousTotal() {
        return previousTotal;
    }

    public BigDecimal getCurrentTotal() {
        return currentTotal;
    }

    public BigDecimal getDelta() {
        return delta;
    }

    public String getCurrency() {
        return currency;
    }

    public BudgetStatus getBudgetStatus() {
        return budgetStatus;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public BigDecimal getBudgetUsedPercent() {
        return budgetUsedPercent;
    }

    public BigDecimal getBudgetWarningThreshold() {
        return budgetWarningThreshold;
    }
}
