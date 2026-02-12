package com.cloudcity.platform.api.dto;

import java.util.UUID;

public class GraphHealthIssueResponse {
    private UUID nodeId;
    private String nodeType;
    private String nodeName;
    private String issue;

    public GraphHealthIssueResponse(UUID nodeId, String nodeType, String nodeName, String issue) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.nodeName = nodeName;
        this.issue = issue;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getIssue() {
        return issue;
    }
}
