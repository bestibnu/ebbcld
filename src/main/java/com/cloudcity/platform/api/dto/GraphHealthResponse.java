package com.cloudcity.platform.api.dto;

import java.util.List;

public class GraphHealthResponse {
    private int totalNodes;
    private int totalEdges;
    private int orphanNodeCount;
    private int misconfiguredNodeCount;
    private List<GraphHealthIssueResponse> orphanNodes;
    private List<GraphHealthIssueResponse> misconfiguredNodes;

    public GraphHealthResponse(int totalNodes,
                               int totalEdges,
                               int orphanNodeCount,
                               int misconfiguredNodeCount,
                               List<GraphHealthIssueResponse> orphanNodes,
                               List<GraphHealthIssueResponse> misconfiguredNodes) {
        this.totalNodes = totalNodes;
        this.totalEdges = totalEdges;
        this.orphanNodeCount = orphanNodeCount;
        this.misconfiguredNodeCount = misconfiguredNodeCount;
        this.orphanNodes = orphanNodes;
        this.misconfiguredNodes = misconfiguredNodes;
    }

    public int getTotalNodes() {
        return totalNodes;
    }

    public int getTotalEdges() {
        return totalEdges;
    }

    public int getOrphanNodeCount() {
        return orphanNodeCount;
    }

    public int getMisconfiguredNodeCount() {
        return misconfiguredNodeCount;
    }

    public List<GraphHealthIssueResponse> getOrphanNodes() {
        return orphanNodes;
    }

    public List<GraphHealthIssueResponse> getMisconfiguredNodes() {
        return misconfiguredNodes;
    }
}
