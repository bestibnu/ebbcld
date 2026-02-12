package com.cloudcity.platform.infrastructure.aws;

public class AwsDiscoveredResource {
    private final String id;
    private final String name;
    private final String region;
    private final String vpcId;
    private final String subnetId;

    public AwsDiscoveredResource(String id, String name, String region, String vpcId, String subnetId) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.vpcId = vpcId;
        this.subnetId = subnetId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
    }

    public String getVpcId() {
        return vpcId;
    }

    public String getSubnetId() {
        return subnetId;
    }
}
