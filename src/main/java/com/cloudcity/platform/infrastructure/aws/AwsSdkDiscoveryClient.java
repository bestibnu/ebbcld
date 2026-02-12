package com.cloudcity.platform.infrastructure.aws;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;

@Component
@Profile("!stub-aws")
public class AwsSdkDiscoveryClient implements AwsDiscoveryClient {
    @Override
    public List<AwsDiscoveredResource> listVpc(String region) {
        try (Ec2Client client = Ec2Client.builder().region(Region.of(region)).build()) {
            List<AwsDiscoveredResource> results = new ArrayList<>();
            for (Vpc vpc : client.describeVpcs(DescribeVpcsRequest.builder().build()).vpcs()) {
                results.add(new AwsDiscoveredResource(vpc.vpcId(), extractName(vpc.tags(), "vpc"), region, null, null));
            }
            return results;
        }
    }

    @Override
    public List<AwsDiscoveredResource> listSubnets(String region) {
        try (Ec2Client client = Ec2Client.builder().region(Region.of(region)).build()) {
            List<AwsDiscoveredResource> results = new ArrayList<>();
            for (Subnet subnet : client.describeSubnets(DescribeSubnetsRequest.builder().build()).subnets()) {
                results.add(new AwsDiscoveredResource(
                        subnet.subnetId(),
                        extractName(subnet.tags(), "subnet"),
                        region,
                        subnet.vpcId(),
                        null
                ));
            }
            return results;
        }
    }

    @Override
    public List<AwsDiscoveredResource> listInstances(String region) {
        try (Ec2Client client = Ec2Client.builder().region(Region.of(region)).build()) {
            List<AwsDiscoveredResource> results = new ArrayList<>();
            for (var reservation : client.describeInstances(DescribeInstancesRequest.builder().build()).reservations()) {
                for (Instance instance : reservation.instances()) {
                    results.add(new AwsDiscoveredResource(
                            instance.instanceId(),
                            extractName(instance.tags(), "ec2"),
                            region,
                            instance.vpcId(),
                            instance.subnetId()
                    ));
                }
            }
            return results;
        }
    }

    @Override
    public List<AwsDiscoveredResource> listSecurityGroups(String region) {
        try (Ec2Client client = Ec2Client.builder().region(Region.of(region)).build()) {
            List<AwsDiscoveredResource> results = new ArrayList<>();
            for (SecurityGroup sg : client.describeSecurityGroups().securityGroups()) {
                results.add(new AwsDiscoveredResource(
                        sg.groupId(),
                        sg.groupName(),
                        region,
                        sg.vpcId(),
                        null
                ));
            }
            return results;
        }
    }

    @Override
    public List<AwsDiscoveredResource> listLoadBalancers(String region) {
        try (ElasticLoadBalancingV2Client client = ElasticLoadBalancingV2Client.builder()
                .region(Region.of(region))
                .build()) {
            List<AwsDiscoveredResource> results = new ArrayList<>();
            for (LoadBalancer lb : client.describeLoadBalancers(DescribeLoadBalancersRequest.builder().build())
                    .loadBalancers()) {
                String subnetId = lb.availabilityZones().isEmpty()
                        ? null
                        : lb.availabilityZones().get(0).subnetId();
                results.add(new AwsDiscoveredResource(
                        lb.loadBalancerArn(),
                        lb.loadBalancerName(),
                        region,
                        lb.vpcId(),
                        subnetId
                ));
            }
            return results;
        }
    }

    @Override
    public List<AwsDiscoveredResource> listRdsInstances(String region) {
        try (RdsClient client = RdsClient.builder().region(Region.of(region)).build()) {
            List<AwsDiscoveredResource> results = new ArrayList<>();
            for (DBInstance db : client.describeDBInstances(DescribeDbInstancesRequest.builder().build()).dbInstances()) {
                String subnetId = db.dbSubnetGroup() == null || db.dbSubnetGroup().subnets().isEmpty()
                        ? null
                        : db.dbSubnetGroup().subnets().get(0).subnetIdentifier();
                results.add(new AwsDiscoveredResource(
                        db.dbInstanceIdentifier(),
                        db.dbInstanceIdentifier(),
                        region,
                        db.dbSubnetGroup() == null ? null : db.dbSubnetGroup().vpcId(),
                        subnetId
                ));
            }
            return results;
        }
    }

    private String extractName(List<Tag> tags, String fallbackPrefix) {
        if (tags == null || tags.isEmpty()) {
            return fallbackPrefix;
        }
        for (Tag tag : tags) {
            if ("Name".equalsIgnoreCase(tag.key()) && tag.value() != null && !tag.value().isBlank()) {
                return tag.value();
            }
        }
        return fallbackPrefix;
    }
}
