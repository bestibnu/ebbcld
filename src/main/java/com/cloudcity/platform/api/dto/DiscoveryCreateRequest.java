package com.cloudcity.platform.api.dto;

import com.cloudcity.platform.domain.CloudProvider;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class DiscoveryCreateRequest {
    @NotNull
    private CloudProvider provider;

    private String accountId;

    private String roleArn;

    private String externalId;

    private List<String> regions;

    public CloudProvider getProvider() {
        return provider;
    }

    public void setProvider(CloudProvider provider) {
        this.provider = provider;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public List<String> getRegions() {
        return regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
    }
}
