package com.cloudcity.platform.api.dto;

import java.math.BigDecimal;

public class PipelineCheckRequest {
    private BigDecimal projectedMonthlyDelta;
    private Boolean strictMode;

    public BigDecimal getProjectedMonthlyDelta() {
        return projectedMonthlyDelta;
    }

    public void setProjectedMonthlyDelta(BigDecimal projectedMonthlyDelta) {
        this.projectedMonthlyDelta = projectedMonthlyDelta;
    }

    public Boolean getStrictMode() {
        return strictMode;
    }

    public void setStrictMode(Boolean strictMode) {
        this.strictMode = strictMode;
    }
}
