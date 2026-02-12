package com.cloudcity.platform.service;

import com.cloudcity.platform.api.dto.GraphCostHotspotResponse;
import com.cloudcity.platform.api.dto.GraphHealthIssueResponse;
import com.cloudcity.platform.api.dto.GraphHealthResponse;
import com.cloudcity.platform.api.dto.GraphSummaryResponse;
import com.cloudcity.platform.api.dto.ResourceEdgeRequest;
import com.cloudcity.platform.api.dto.ResourceNodeRequest;
import com.cloudcity.platform.api.dto.ResourceNodeUpdateRequest;
import com.cloudcity.platform.domain.Project;
import com.cloudcity.platform.domain.RelationType;
import com.cloudcity.platform.domain.ResourceEdge;
import com.cloudcity.platform.domain.ResourceNode;
import com.cloudcity.platform.domain.ResourceType;
import com.cloudcity.platform.repository.ProjectRepository;
import com.cloudcity.platform.repository.ResourceEdgeRepository;
import com.cloudcity.platform.repository.ResourceNodeRepository;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResourceGraphService {
    private static final Map<ResourceType, Set<ResourceType>> CONTAINS_RULES;
    private static final Map<ResourceType, Set<ResourceType>> CONNECTS_RULES;

    static {
        Map<ResourceType, Set<ResourceType>> contains = new EnumMap<>(ResourceType.class);
        contains.put(ResourceType.REGION, EnumSet.of(ResourceType.VPC, ResourceType.S3));
        contains.put(ResourceType.VPC, EnumSet.of(ResourceType.SUBNET, ResourceType.SG));
        contains.put(ResourceType.SUBNET, EnumSet.of(ResourceType.EC2, ResourceType.RDS, ResourceType.ELB));
        CONTAINS_RULES = Map.copyOf(contains);

        Map<ResourceType, Set<ResourceType>> connects = new EnumMap<>(ResourceType.class);
        connects.put(ResourceType.SG, EnumSet.of(ResourceType.EC2, ResourceType.RDS, ResourceType.ELB));
        CONNECTS_RULES = Map.copyOf(connects);
    }

    private final ProjectRepository projectRepository;
    private final ResourceNodeRepository nodeRepository;
    private final ResourceEdgeRepository edgeRepository;

    public ResourceGraphService(ProjectRepository projectRepository,
                                ResourceNodeRepository nodeRepository,
                                ResourceEdgeRepository edgeRepository) {
        this.projectRepository = projectRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    @Transactional
    public ResourceNode createNode(UUID projectId, ResourceNodeRequest request) {
        Project project = findProject(projectId);
        ResourceNode node = new ResourceNode();
        node.setProject(project);
        node.setProvider(request.getProvider());
        node.setType(request.getType());
        node.setName(request.getName());
        node.setRegion(request.getRegion());
        node.setZone(request.getZone());
        node.setState(request.getState());
        node.setSource(request.getSource());
        node.setCostEstimate(request.getCostEstimate());
        node.setMetadataJson(request.getMetadataJson());
        return nodeRepository.save(node);
    }

    @Transactional
    public ResourceNode updateNode(UUID projectId, UUID nodeId, ResourceNodeUpdateRequest request) {
        ResourceNode node = nodeRepository.findByIdAndProjectId(nodeId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found"));

        if (request.getProvider() != null) {
            node.setProvider(request.getProvider());
        }
        if (request.getType() != null) {
            node.setType(request.getType());
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            node.setName(request.getName());
        }
        if (request.getRegion() != null) {
            node.setRegion(request.getRegion());
        }
        if (request.getZone() != null) {
            node.setZone(request.getZone());
        }
        if (request.getState() != null) {
            node.setState(request.getState());
        }
        if (request.getSource() != null) {
            node.setSource(request.getSource());
        }
        if (request.getCostEstimate() != null) {
            node.setCostEstimate(request.getCostEstimate());
        }
        if (request.getMetadataJson() != null) {
            node.setMetadataJson(request.getMetadataJson());
        }

        return nodeRepository.save(node);
    }

    @Transactional
    public void deleteNode(UUID projectId, UUID nodeId) {
        ResourceNode node = nodeRepository.findByIdAndProjectId(nodeId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found"));
        nodeRepository.delete(node);
    }

    @Transactional
    public ResourceEdge createEdge(UUID projectId, ResourceEdgeRequest request) {
        Project project = findProject(projectId);
        ResourceNode fromNode = nodeRepository.findByIdAndProjectId(request.getFromNodeId(), projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "From node not found"));
        ResourceNode toNode = nodeRepository.findByIdAndProjectId(request.getToNodeId(), projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "To node not found"));

        validateRelation(fromNode.getType(), toNode.getType(), request.getRelationType());

        ResourceEdge edge = new ResourceEdge();
        edge.setProject(project);
        edge.setFromNode(fromNode);
        edge.setToNode(toNode);
        edge.setRelationType(request.getRelationType());
        return edgeRepository.save(edge);
    }

    @Transactional
    public void deleteEdge(UUID projectId, UUID edgeId) {
        ResourceEdge edge = edgeRepository.findByIdAndProjectId(edgeId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Edge not found"));
        edgeRepository.delete(edge);
    }

    @Transactional(readOnly = true)
    public List<ResourceNode> getNodes(UUID projectId) {
        findProject(projectId);
        return nodeRepository.findAllByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<ResourceEdge> getEdges(UUID projectId) {
        findProject(projectId);
        return edgeRepository.findAllByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public GraphSummaryResponse getGraphSummary(UUID projectId) {
        findProject(projectId);
        List<ResourceNode> nodes = nodeRepository.findAllByProjectId(projectId);
        List<ResourceEdge> edges = edgeRepository.findAllByProjectId(projectId);

        Map<String, Long> nodeCountByType = new HashMap<>();
        Map<String, Long> nodeCountByRegion = new HashMap<>();
        Map<String, java.math.BigDecimal> estimatedCostByType = new HashMap<>();
        Map<String, java.math.BigDecimal> estimatedCostByRegion = new HashMap<>();
        java.math.BigDecimal totalEstimatedCost = java.math.BigDecimal.ZERO;

        for (ResourceNode node : nodes) {
            String typeKey = node.getType().name();
            String regionKey = (node.getRegion() == null || node.getRegion().isBlank()) ? "unknown" : node.getRegion();
            java.math.BigDecimal cost = node.getCostEstimate() == null ? java.math.BigDecimal.ZERO : node.getCostEstimate();

            nodeCountByType.put(typeKey, nodeCountByType.getOrDefault(typeKey, 0L) + 1);
            nodeCountByRegion.put(regionKey, nodeCountByRegion.getOrDefault(regionKey, 0L) + 1);
            estimatedCostByType.put(typeKey, estimatedCostByType.getOrDefault(typeKey, java.math.BigDecimal.ZERO).add(cost));
            estimatedCostByRegion.put(regionKey, estimatedCostByRegion.getOrDefault(regionKey, java.math.BigDecimal.ZERO).add(cost));
            totalEstimatedCost = totalEstimatedCost.add(cost);
        }

        List<GraphCostHotspotResponse> topCostTypes = toTopHotspots(estimatedCostByType);
        List<GraphCostHotspotResponse> topCostRegions = toTopHotspots(estimatedCostByRegion);

        return new GraphSummaryResponse(
                nodes.size(),
                edges.size(),
                nodeCountByType,
                nodeCountByRegion,
                totalEstimatedCost,
                estimatedCostByType,
                estimatedCostByRegion,
                topCostTypes,
                topCostRegions
        );
    }

    @Transactional(readOnly = true)
    public GraphHealthResponse getGraphHealth(UUID projectId) {
        findProject(projectId);
        List<ResourceNode> nodes = nodeRepository.findAllByProjectId(projectId);
        List<ResourceEdge> edges = edgeRepository.findAllByProjectId(projectId);

        Set<UUID> connectedNodeIds = new HashSet<>();
        for (ResourceEdge edge : edges) {
            connectedNodeIds.add(edge.getFromNode().getId());
            connectedNodeIds.add(edge.getToNode().getId());
        }

        List<GraphHealthIssueResponse> orphanNodes = new java.util.ArrayList<>();
        List<GraphHealthIssueResponse> misconfiguredNodes = new java.util.ArrayList<>();
        for (ResourceNode node : nodes) {
            if (!connectedNodeIds.contains(node.getId()) && node.getType() != ResourceType.REGION) {
                orphanNodes.add(new GraphHealthIssueResponse(
                        node.getId(),
                        node.getType().name(),
                        node.getName(),
                        "Node has no incoming/outgoing relationship"
                ));
            }
            String configIssue = detectConfigIssue(node);
            if (configIssue != null) {
                misconfiguredNodes.add(new GraphHealthIssueResponse(
                        node.getId(),
                        node.getType().name(),
                        node.getName(),
                        configIssue
                ));
            }
        }

        return new GraphHealthResponse(
                nodes.size(),
                edges.size(),
                orphanNodes.size(),
                misconfiguredNodes.size(),
                orphanNodes,
                misconfiguredNodes
        );
    }

    private String detectConfigIssue(ResourceNode node) {
        if (node.getType() == ResourceType.VPC && (node.getRegion() == null || node.getRegion().isBlank())) {
            return "VPC is missing region";
        }
        if (node.getType() == ResourceType.SUBNET && (node.getRegion() == null || node.getRegion().isBlank())) {
            return "Subnet is missing region";
        }
        if ((node.getType() == ResourceType.EC2 || node.getType() == ResourceType.RDS || node.getType() == ResourceType.ELB)
                && (node.getRegion() == null || node.getRegion().isBlank())) {
            return node.getType().name() + " is missing region";
        }
        return null;
    }

    private List<GraphCostHotspotResponse> toTopHotspots(Map<String, java.math.BigDecimal> costByKey) {
        return costByKey.entrySet()
                .stream()
                .sorted(Comparator
                        .<Map.Entry<String, java.math.BigDecimal>, java.math.BigDecimal>comparing(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(3)
                .map(entry -> new GraphCostHotspotResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private void validateRelation(ResourceType fromType, ResourceType toType, RelationType relationType) {
        if (relationType == RelationType.DEPENDS_ON) {
            return;
        }

        if (relationType == RelationType.CONTAINS) {
            Set<ResourceType> allowed = CONTAINS_RULES.get(fromType);
            if (allowed == null || !allowed.contains(toType)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid CONTAINS relation from " + fromType + " to " + toType);
            }
            return;
        }

        if (relationType == RelationType.CONNECTS) {
            Set<ResourceType> allowed = CONNECTS_RULES.get(fromType);
            if (allowed == null || !allowed.contains(toType)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid CONNECTS relation from " + fromType + " to " + toType);
            }
            return;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported relation type");
    }
}
