package com.cloudcity.platform.api;

import com.cloudcity.platform.api.dto.GraphResponse;
import com.cloudcity.platform.api.dto.GraphHealthResponse;
import com.cloudcity.platform.api.dto.GraphSummaryResponse;
import com.cloudcity.platform.api.dto.ResourceEdgeRequest;
import com.cloudcity.platform.api.dto.ResourceEdgeResponse;
import com.cloudcity.platform.api.dto.ResourceNodeRequest;
import com.cloudcity.platform.api.dto.ResourceNodeResponse;
import com.cloudcity.platform.api.dto.ResourceNodeUpdateRequest;
import com.cloudcity.platform.domain.ResourceEdge;
import com.cloudcity.platform.domain.ResourceNode;
import com.cloudcity.platform.service.ResourceGraphService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
@Tag(name = "Resource Graph")
public class ResourceGraphController {
    private final ResourceGraphService graphService;

    public ResourceGraphController(ResourceGraphService graphService) {
        this.graphService = graphService;
    }

    @PostMapping("/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceNodeResponse createNode(@PathVariable UUID projectId, @Valid @RequestBody ResourceNodeRequest request) {
        ResourceNode saved = graphService.createNode(projectId, request);
        return toNodeResponse(saved);
    }

    @PatchMapping("/nodes/{nodeId}")
    public ResourceNodeResponse updateNode(@PathVariable UUID projectId,
                                           @PathVariable UUID nodeId,
                                           @RequestBody ResourceNodeUpdateRequest request) {
        ResourceNode saved = graphService.updateNode(projectId, nodeId, request);
        return toNodeResponse(saved);
    }

    @PostMapping("/edges")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceEdgeResponse createEdge(@PathVariable UUID projectId, @Valid @RequestBody ResourceEdgeRequest request) {
        ResourceEdge saved = graphService.createEdge(projectId, request);
        return toEdgeResponse(saved);
    }

    @DeleteMapping("/nodes/{nodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNode(@PathVariable UUID projectId, @PathVariable UUID nodeId) {
        graphService.deleteNode(projectId, nodeId);
    }

    @DeleteMapping("/edges/{edgeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEdge(@PathVariable UUID projectId, @PathVariable UUID edgeId) {
        graphService.deleteEdge(projectId, edgeId);
    }

    @GetMapping("/graph")
    public GraphResponse getGraph(@PathVariable UUID projectId) {
        List<ResourceNodeResponse> nodes = graphService.getNodes(projectId)
                .stream()
                .map(this::toNodeResponse)
                .collect(Collectors.toList());
        List<ResourceEdgeResponse> edges = graphService.getEdges(projectId)
                .stream()
                .map(this::toEdgeResponse)
                .collect(Collectors.toList());
        return new GraphResponse(nodes, edges);
    }

    @GetMapping("/graph/summary")
    public GraphSummaryResponse getGraphSummary(@PathVariable UUID projectId) {
        return graphService.getGraphSummary(projectId);
    }

    @GetMapping("/graph/health")
    public GraphHealthResponse getGraphHealth(@PathVariable UUID projectId) {
        return graphService.getGraphHealth(projectId);
    }

    private ResourceNodeResponse toNodeResponse(ResourceNode node) {
        return new ResourceNodeResponse(
                node.getId(),
                node.getProject().getId(),
                node.getProvider(),
                node.getType(),
                node.getName(),
                node.getRegion(),
                node.getZone(),
                node.getState(),
                node.getSource(),
                node.getCostEstimate(),
                node.getMetadataJson(),
                node.getCreatedAt(),
                node.getUpdatedAt()
        );
    }

    private ResourceEdgeResponse toEdgeResponse(ResourceEdge edge) {
        return new ResourceEdgeResponse(
                edge.getId(),
                edge.getProject().getId(),
                edge.getFromNode().getId(),
                edge.getToNode().getId(),
                edge.getRelationType(),
                edge.getCreatedAt()
        );
    }
}
