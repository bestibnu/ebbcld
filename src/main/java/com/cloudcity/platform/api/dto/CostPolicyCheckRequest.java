package com.cloudcity.platform.api.dto;

import java.math.BigDecimal;

public class CostPolicyCheckRequest {
    private BigDecimal projectedMonthlyDelta;

    public BigDecimal getProjectedMonthlyDelta() {
        return projectedMonthlyDelta;
    }

    public void setProjectedMonthlyDelta(BigDecimal projectedMonthlyDelta) {
        this.projectedMonthlyDelta = projectedMonthlyDelta;
    }
}
