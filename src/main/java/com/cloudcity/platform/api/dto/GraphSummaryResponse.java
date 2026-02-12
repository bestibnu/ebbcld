package com.cloudcity.platform.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class GraphSummaryResponse {
    private int totalNodes;
    private int totalEdges;
    private Map<String, Long> nodeCountByType;
    private Map<String, Long> nodeCountByRegion;
    private BigDecimal totalEstimatedCost;
    private Map<String, BigDecimal> estimatedCostByType;
    private Map<String, BigDecimal> estimatedCostByRegion;
    private List<GraphCostHotspotResponse> topCostTypes;
    private List<GraphCostHotspotResponse> topCostRegions;

    public GraphSummaryResponse(int totalNodes,
                                int totalEdges,
                                Map<String, Long> nodeCountByType,
                                Map<String, Long> nodeCountByRegion,
                                BigDecimal totalEstimatedCost,
                                Map<String, BigDecimal> estimatedCostByType,
                                Map<String, BigDecimal> estimatedCostByRegion,
                                List<GraphCostHotspotResponse> topCostTypes,
                                List<GraphCostHotspotResponse> topCostRegions) {
        this.totalNodes = totalNodes;
        this.totalEdges = totalEdges;
        this.nodeCountByType = nodeCountByType;
        this.nodeCountByRegion = nodeCountByRegion;
        this.totalEstimatedCost = totalEstimatedCost;
        this.estimatedCostByType = estimatedCostByType;
        this.estimatedCostByRegion = estimatedCostByRegion;
        this.topCostTypes = topCostTypes;
        this.topCostRegions = topCostRegions;
    }

    public int getTotalNodes() {
        return totalNodes;
    }

    public int getTotalEdges() {
        return totalEdges;
    }

    public Map<String, Long> getNodeCountByType() {
        return nodeCountByType;
    }

    public Map<String, Long> getNodeCountByRegion() {
        return nodeCountByRegion;
    }

    public BigDecimal getTotalEstimatedCost() {
        return totalEstimatedCost;
    }

    public Map<String, BigDecimal> getEstimatedCostByType() {
        return estimatedCostByType;
    }

    public Map<String, BigDecimal> getEstimatedCostByRegion() {
        return estimatedCostByRegion;
    }

    public List<GraphCostHotspotResponse> getTopCostTypes() {
        return topCostTypes;
    }

    public List<GraphCostHotspotResponse> getTopCostRegions() {
        return topCostRegions;
    }
}
