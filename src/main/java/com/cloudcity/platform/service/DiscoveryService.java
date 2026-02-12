package com.cloudcity.platform.service;

import com.cloudcity.platform.api.dto.DiscoveryCreateRequest;
import com.cloudcity.platform.domain.DiscoveryRun;
import com.cloudcity.platform.domain.Project;
import com.cloudcity.platform.repository.DiscoveryRunRepository;
import com.cloudcity.platform.repository.ProjectRepository;
import com.cloudcity.platform.repository.ResourceNodeRepository;
import com.cloudcity.platform.repository.ResourceEdgeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.cloudcity.platform.infrastructure.aws.AwsDiscoveryClient;
import com.cloudcity.platform.infrastructure.aws.AwsDiscoveredResource;
import com.cloudcity.platform.domain.ResourceNode;
import com.cloudcity.platform.domain.ResourceType;
import com.cloudcity.platform.domain.ResourceSource;
import com.cloudcity.platform.domain.CloudProvider;
import com.cloudcity.platform.domain.ResourceEdge;
import com.cloudcity.platform.domain.RelationType;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DiscoveryService {
    private static final String STATUS_QUEUED = "QUEUED";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ProjectRepository projectRepository;
    private final DiscoveryRunRepository discoveryRunRepository;
    private final ResourceNodeRepository resourceNodeRepository;
    private final ResourceEdgeRepository resourceEdgeRepository;
    private final ObjectMapper objectMapper;
    private final TaskExecutor discoveryTaskExecutor;
    private final boolean asyncEnabled;
    private final AwsDiscoveryClient awsDiscoveryClient;

    public DiscoveryService(ProjectRepository projectRepository,
                            DiscoveryRunRepository discoveryRunRepository,
                            ResourceNodeRepository resourceNodeRepository,
                            ResourceEdgeRepository resourceEdgeRepository,
                            ObjectMapper objectMapper,
                            TaskExecutor discoveryTaskExecutor,
                            @Value("${cloudcity.discovery.async:true}") boolean asyncEnabled,
                            AwsDiscoveryClient awsDiscoveryClient) {
        this.projectRepository = projectRepository;
        this.discoveryRunRepository = discoveryRunRepository;
        this.resourceNodeRepository = resourceNodeRepository;
        this.resourceEdgeRepository = resourceEdgeRepository;
        this.objectMapper = objectMapper;
        this.discoveryTaskExecutor = discoveryTaskExecutor;
        this.asyncEnabled = asyncEnabled;
        this.awsDiscoveryClient = awsDiscoveryClient;
    }

    @Transactional
    public DiscoveryRun createDiscovery(UUID projectId, DiscoveryCreateRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        DiscoveryRun run = new DiscoveryRun();
        run.setProject(project);
        run.setProvider(request.getProvider());
        run.setAccountId(request.getAccountId());
        run.setStatus(STATUS_QUEUED);
        run.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        run.setSummaryJson(serializeSummary(request));

        return discoveryRunRepository.save(run);
    }

    @Transactional(readOnly = true)
    public DiscoveryRun getDiscovery(UUID projectId, UUID discoveryId) {
        return discoveryRunRepository.findByIdAndProjectId(discoveryId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discovery run not found"));
    }

    @Transactional
    public DiscoveryRun executeDiscovery(UUID projectId, UUID discoveryId) {
        DiscoveryRun run = discoveryRunRepository.findByIdAndProjectId(discoveryId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discovery run not found"));

        run.setStatus(STATUS_QUEUED);
        run.setFinishedAt(null);
        run.setSummaryJson(updateProgress(run.getSummaryJson(), 10, false));
        DiscoveryRun queued = discoveryRunRepository.save(run);

        if (asyncEnabled) {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        discoveryTaskExecutor.execute(() -> runDiscovery(projectId, discoveryId));
                    }
                });
            } else {
                discoveryTaskExecutor.execute(() -> runDiscovery(projectId, discoveryId));
            }
            return queued;
        }

        runDiscovery(projectId, discoveryId);
        return discoveryRunRepository.findByIdAndProjectId(discoveryId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discovery run not found"));
    }

    private String serializeSummary(DiscoveryCreateRequest request) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("regions", request.getRegions());
        summary.put("roleArn", request.getRoleArn());
        summary.put("externalId", request.getExternalId());
        summary.put("progress", 0);
        summary.put("executed", false);
        summary.put("stub", true);
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid discovery request");
        }
    }

    private String updateProgress(String summaryJson, int progress, boolean executed) {
        Map<String, Object> summary = new HashMap<>();
        if (summaryJson != null && !summaryJson.isBlank()) {
            try {
                Object parsed = objectMapper.readValue(summaryJson, Object.class);
                if (parsed instanceof Map) {
                    summary.putAll((Map<String, Object>) parsed);
                } else if (parsed instanceof String) {
                    Object nested = objectMapper.readValue((String) parsed, Object.class);
                    if (nested instanceof Map) {
                        summary.putAll((Map<String, Object>) nested);
                    }
                }
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid discovery summary");
            }
        }
        summary.put("progress", progress);
        summary.put("executed", executed);
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid discovery summary");
        }
    }

    private void runDiscovery(UUID projectId, UUID discoveryId) {
        DiscoveryRun run = discoveryRunRepository.findByIdAndProjectId(discoveryId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discovery run not found"));
        run.setStatus(STATUS_RUNNING);
        run.setSummaryJson(updateProgress(run.getSummaryJson(), 60, false));
        discoveryRunRepository.save(run);

        List<String> regions = extractRegions(run.getSummaryJson());
        if (regions.isEmpty()) {
            regions = List.of("us-east-1");
        }
        for (String region : regions) {
            ingestRegion(projectId, region);
        }

        run.setStatus(STATUS_COMPLETED);
        run.setFinishedAt(OffsetDateTime.now(ZoneOffset.UTC));
        run.setSummaryJson(updateProgress(run.getSummaryJson(), 100, true));
        discoveryRunRepository.save(run);
    }

    private void ingestRegion(UUID projectId, String region) {
        Map<String, ResourceNode> vpcsById = new HashMap<>();
        Map<String, ResourceNode> subnetsById = new HashMap<>();

        for (AwsDiscoveredResource vpc : awsDiscoveryClient.listVpc(region)) {
            ResourceNode node = new ResourceNode();
            node.setProject(projectRepository.getReferenceById(projectId));
            node.setProvider(CloudProvider.AWS);
            node.setType(ResourceType.VPC);
            node.setName(vpc.getName());
            node.setRegion(region);
            node.setSource(ResourceSource.DISCOVERED);
            node.setMetadataJson("{\"awsId\":\"" + vpc.getId() + "\"}");
            ResourceNode saved = resourceNodeRepository.save(node);
            vpcsById.put(vpc.getId(), saved);
        }
        for (AwsDiscoveredResource subnet : awsDiscoveryClient.listSubnets(region)) {
            ResourceNode node = new ResourceNode();
            node.setProject(projectRepository.getReferenceById(projectId));
            node.setProvider(CloudProvider.AWS);
            node.setType(ResourceType.SUBNET);
            node.setName(subnet.getName());
            node.setRegion(region);
            node.setSource(ResourceSource.DISCOVERED);
            node.setMetadataJson(buildMetadata(subnet.getId(), subnet.getVpcId(), null));
            ResourceNode saved = resourceNodeRepository.save(node);
            subnetsById.put(subnet.getId(), saved);

            ResourceNode vpcNode = subnet.getVpcId() == null ? null : vpcsById.get(subnet.getVpcId());
            if (vpcNode != null) {
                ResourceEdge edge = new ResourceEdge();
                edge.setProject(projectRepository.getReferenceById(projectId));
                edge.setFromNode(vpcNode);
                edge.setToNode(saved);
                edge.setRelationType(RelationType.CONTAINS);
                resourceEdgeRepository.save(edge);
            }
        }
        for (AwsDiscoveredResource instance : awsDiscoveryClient.listInstances(region)) {
            ResourceNode node = new ResourceNode();
            node.setProject(projectRepository.getReferenceById(projectId));
            node.setProvider(CloudProvider.AWS);
            node.setType(ResourceType.EC2);
            node.setName(instance.getName());
            node.setRegion(region);
            node.setSource(ResourceSource.DISCOVERED);
            node.setMetadataJson(buildMetadata(instance.getId(), instance.getVpcId(), instance.getSubnetId()));
            ResourceNode saved = resourceNodeRepository.save(node);

            ResourceNode subnetNode = instance.getSubnetId() == null ? null : subnetsById.get(instance.getSubnetId());
            if (subnetNode != null) {
                ResourceEdge edge = new ResourceEdge();
                edge.setProject(projectRepository.getReferenceById(projectId));
                edge.setFromNode(subnetNode);
                edge.setToNode(saved);
                edge.setRelationType(RelationType.CONTAINS);
                resourceEdgeRepository.save(edge);
            }
        }

        for (AwsDiscoveredResource sg : awsDiscoveryClient.listSecurityGroups(region)) {
            ResourceNode node = new ResourceNode();
            node.setProject(projectRepository.getReferenceById(projectId));
            node.setProvider(CloudProvider.AWS);
            node.setType(ResourceType.SG);
            node.setName(sg.getName());
            node.setRegion(region);
            node.setSource(ResourceSource.DISCOVERED);
            node.setMetadataJson(buildMetadata(sg.getId(), sg.getVpcId(), null));
            ResourceNode saved = resourceNodeRepository.save(node);

            ResourceNode vpcNode = sg.getVpcId() == null ? null : vpcsById.get(sg.getVpcId());
            if (vpcNode != null) {
                ResourceEdge edge = new ResourceEdge();
                edge.setProject(projectRepository.getReferenceById(projectId));
                edge.setFromNode(vpcNode);
                edge.setToNode(saved);
                edge.setRelationType(RelationType.CONTAINS);
                resourceEdgeRepository.save(edge);
            }
        }

        for (AwsDiscoveredResource lb : awsDiscoveryClient.listLoadBalancers(region)) {
            ResourceNode node = new ResourceNode();
            node.setProject(projectRepository.getReferenceById(projectId));
            node.setProvider(CloudProvider.AWS);
            node.setType(ResourceType.ELB);
            node.setName(lb.getName());
            node.setRegion(region);
            node.setSource(ResourceSource.DISCOVERED);
            node.setMetadataJson(buildMetadata(lb.getId(), lb.getVpcId(), lb.getSubnetId()));
            ResourceNode saved = resourceNodeRepository.save(node);

            ResourceNode subnetNode = lb.getSubnetId() == null ? null : subnetsById.get(lb.getSubnetId());
            if (subnetNode != null) {
                ResourceEdge edge = new ResourceEdge();
                edge.setProject(projectRepository.getReferenceById(projectId));
                edge.setFromNode(subnetNode);
                edge.setToNode(saved);
                edge.setRelationType(RelationType.CONTAINS);
                resourceEdgeRepository.save(edge);
            }
        }

        for (AwsDiscoveredResource rds : awsDiscoveryClient.listRdsInstances(region)) {
            ResourceNode node = new ResourceNode();
            node.setProject(projectRepository.getReferenceById(projectId));
            node.setProvider(CloudProvider.AWS);
            node.setType(ResourceType.RDS);
            node.setName(rds.getName());
            node.setRegion(region);
            node.setSource(ResourceSource.DISCOVERED);
            node.setMetadataJson(buildMetadata(rds.getId(), rds.getVpcId(), rds.getSubnetId()));
            ResourceNode saved = resourceNodeRepository.save(node);

            ResourceNode subnetNode = rds.getSubnetId() == null ? null : subnetsById.get(rds.getSubnetId());
            if (subnetNode != null) {
                ResourceEdge edge = new ResourceEdge();
                edge.setProject(projectRepository.getReferenceById(projectId));
                edge.setFromNode(subnetNode);
                edge.setToNode(saved);
                edge.setRelationType(RelationType.CONTAINS);
                resourceEdgeRepository.save(edge);
            }
        }
    }

    private List<String> extractRegions(String summaryJson) {
        if (summaryJson == null || summaryJson.isBlank()) {
            return List.of();
        }
        try {
            Object parsed = objectMapper.readValue(summaryJson, Object.class);
            Map<String, Object> summary = parsed instanceof Map ? (Map<String, Object>) parsed : Map.of();
            Object regions = summary.get("regions");
            if (regions instanceof List) {
                return (List<String>) regions;
            }
            return List.of();
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String buildMetadata(String awsId, String vpcId, String subnetId) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"awsId\":\"").append(awsId).append("\"");
        if (vpcId != null && !vpcId.isBlank()) {
            builder.append(",\"vpcId\":\"").append(vpcId).append("\"");
        }
        if (subnetId != null && !subnetId.isBlank()) {
            builder.append(",\"subnetId\":\"").append(subnetId).append("\"");
        }
        builder.append("}");
        return builder.toString();
    }
}
