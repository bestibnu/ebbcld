package com.cloudcity.platform.api.dto;

public class TerraformApprovalRequest {
    private boolean approved;
    private String reason;

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
