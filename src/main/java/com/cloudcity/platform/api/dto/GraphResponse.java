package com.cloudcity.platform.api.dto;

import java.util.List;

public class GraphResponse {
    private List<ResourceNodeResponse> nodes;
    private List<ResourceEdgeResponse> edges;

    public GraphResponse(List<ResourceNodeResponse> nodes, List<ResourceEdgeResponse> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<ResourceNodeResponse> getNodes() {
        return nodes;
    }

    public List<ResourceEdgeResponse> getEdges() {
        return edges;
    }
}
