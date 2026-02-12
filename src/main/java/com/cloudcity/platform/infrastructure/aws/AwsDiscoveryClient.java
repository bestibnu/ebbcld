package com.cloudcity.platform.infrastructure.aws;

import java.util.List;

public interface AwsDiscoveryClient {
    List<AwsDiscoveredResource> listVpc(String region);

    List<AwsDiscoveredResource> listSubnets(String region);

    List<AwsDiscoveredResource> listInstances(String region);

    List<AwsDiscoveredResource> listSecurityGroups(String region);

    List<AwsDiscoveredResource> listLoadBalancers(String region);

    List<AwsDiscoveredResource> listRdsInstances(String region);
}
