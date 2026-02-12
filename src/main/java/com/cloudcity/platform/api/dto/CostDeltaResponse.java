package com.cloudcity.platform.api.dto;

import java.math.BigDecimal;

public class CostDeltaResponse {
    private BigDecimal previousTotal;
    private BigDecimal currentTotal;
    private BigDecimal delta;
    private String currency;

    public CostDeltaResponse(BigDecimal previousTotal, BigDecimal currentTotal, BigDecimal delta, String currency) {
        this.previousTotal = previousTotal;
        this.currentTotal = currentTotal;
        this.delta = delta;
        this.currency = currency;
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
}
