package com.cloudcity.platform.api.dto;

import java.math.BigDecimal;

public class GraphCostHotspotResponse {
    private String key;
    private BigDecimal estimatedCost;

    public GraphCostHotspotResponse(String key, BigDecimal estimatedCost) {
        this.key = key;
        this.estimatedCost = estimatedCost;
    }

    public String getKey() {
        return key;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }
}
