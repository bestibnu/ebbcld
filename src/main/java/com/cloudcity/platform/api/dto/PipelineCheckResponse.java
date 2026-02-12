package com.cloudcity.platform.api.dto;

import java.math.BigDecimal;

public class PipelineCheckResponse {
    private boolean pass;
    private BudgetStatus budgetStatus;
    private boolean requiredApproval;
    private boolean terraformPlanEligible;
    private String reason;
    private String recommendedAction;
    private BigDecimal currentTotal;
    private BigDecimal projectedTotal;
    private BigDecimal monthlyBudget;
    private BigDecimal budgetUsedPercent;
    private BigDecimal budgetWarningThreshold;
    private boolean strictMode;

    public PipelineCheckResponse(boolean pass,
                                 BudgetStatus budgetStatus,
                                 boolean requiredApproval,
                                 boolean terraformPlanEligible,
                                 String reason,
                                 String recommendedAction,
                                 BigDecimal currentTotal,
                                 BigDecimal projectedTotal,
                                 BigDecimal monthlyBudget,
                                 BigDecimal budgetUsedPercent,
                                 BigDecimal budgetWarningThreshold,
                                 boolean strictMode) {
        this.pass = pass;
        this.budgetStatus = budgetStatus;
        this.requiredApproval = requiredApproval;
        this.terraformPlanEligible = terraformPlanEligible;
        this.reason = reason;
        this.recommendedAction = recommendedAction;
        this.currentTotal = currentTotal;
        this.projectedTotal = projectedTotal;
        this.monthlyBudget = monthlyBudget;
        this.budgetUsedPercent = budgetUsedPercent;
        this.budgetWarningThreshold = budgetWarningThreshold;
        this.strictMode = strictMode;
    }

    public boolean isPass() {
        return pass;
    }

    public BudgetStatus getBudgetStatus() {
        return budgetStatus;
    }

    public boolean isRequiredApproval() {
        return requiredApproval;
    }

    public boolean isTerraformPlanEligible() {
        return terraformPlanEligible;
    }

    public String getReason() {
        return reason;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public BigDecimal getCurrentTotal() {
        return currentTotal;
    }

    public BigDecimal getProjectedTotal() {
        return projectedTotal;
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

    public boolean isStrictMode() {
        return strictMode;
    }
}
