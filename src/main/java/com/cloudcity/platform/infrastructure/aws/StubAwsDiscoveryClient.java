package com.cloudcity.platform.infrastructure.aws;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("stub-aws")
public class StubAwsDiscoveryClient implements AwsDiscoveryClient {
    @Override
    public List<AwsDiscoveredResource> listVpc(String region) {
        return List.of(new AwsDiscoveredResource("vpc-123", "vpc-main", region, null, null));
    }

    @Override
    public List<AwsDiscoveredResource> listSubnets(String region) {
        return List.of(
                new AwsDiscoveredResource("subnet-123", "subnet-public", region, "vpc-123", null),
                new AwsDiscoveredResource("subnet-456", "subnet-private", region, "vpc-123", null)
        );
    }

    @Override
    public List<AwsDiscoveredResource> listInstances(String region) {
        return List.of(new AwsDiscoveredResource("i-123", "app-1", region, "vpc-123", "subnet-123"));
    }

    @Override
    public List<AwsDiscoveredResource> listSecurityGroups(String region) {
        return List.of(new AwsDiscoveredResource("sg-123", "sg-app", region, "vpc-123", null));
    }

    @Override
    public List<AwsDiscoveredResource> listLoadBalancers(String region) {
        return List.of(new AwsDiscoveredResource("alb-123", "alb-app", region, "vpc-123", "subnet-123"));
    }

    @Override
    public List<AwsDiscoveredResource> listRdsInstances(String region) {
        return List.of(new AwsDiscoveredResource("rds-123", "db-main", region, "vpc-123", "subnet-123"));
    }
}
