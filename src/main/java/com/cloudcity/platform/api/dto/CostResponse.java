package com.cloudcity.platform.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class CostResponse {
    private BigDecimal totalCost;
    private String currency;
    private String breakdownJson;
    private String pricingVersion;
    private OffsetDateTime createdAt;

    public CostResponse(BigDecimal totalCost, String currency, String breakdownJson, String pricingVersion,
                        OffsetDateTime createdAt) {
        this.totalCost = totalCost;
        this.currency = currency;
        this.breakdownJson = breakdownJson;
        this.pricingVersion = pricingVersion;
        this.createdAt = createdAt;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public String getCurrency() {
        return currency;
    }

    public String getBreakdownJson() {
        return breakdownJson;
    }

    public String getPricingVersion() {
        return pricingVersion;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
