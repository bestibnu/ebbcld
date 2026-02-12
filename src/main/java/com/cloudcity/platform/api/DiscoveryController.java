package com.cloudcity.platform.api;

import com.cloudcity.platform.api.dto.DiscoveryCreateRequest;
import com.cloudcity.platform.api.dto.DiscoveryResponse;
import com.cloudcity.platform.api.dto.DiscoveryStatusResponse;
import com.cloudcity.platform.domain.DiscoveryRun;
import com.cloudcity.platform.service.DiscoveryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/discoveries")
@Tag(name = "Discovery")
public class DiscoveryController {
    private final DiscoveryService discoveryService;
    private final ObjectMapper objectMapper;

    public DiscoveryController(DiscoveryService discoveryService, ObjectMapper objectMapper) {
        this.discoveryService = discoveryService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscoveryResponse createDiscovery(@PathVariable UUID projectId,
                                             @Valid @RequestBody DiscoveryCreateRequest request) {
        DiscoveryRun run = discoveryService.createDiscovery(projectId, request);
        return toResponse(run);
    }

    @GetMapping("/{discoveryId}")
    public DiscoveryResponse getDiscovery(@PathVariable UUID projectId, @PathVariable UUID discoveryId) {
        DiscoveryRun run = discoveryService.getDiscovery(projectId, discoveryId);
        return toResponse(run);
    }

    @GetMapping("/{discoveryId}/status")
    public DiscoveryStatusResponse getStatus(@PathVariable UUID projectId, @PathVariable UUID discoveryId) {
        DiscoveryRun run = discoveryService.getDiscovery(projectId, discoveryId);
        return new DiscoveryStatusResponse(
                run.getId(),
                run.getStatus(),
                extractProgress(run.getSummaryJson()),
                run.getFinishedAt()
        );
    }

    @PostMapping("/{discoveryId}/execute")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DiscoveryResponse executeDiscovery(@PathVariable UUID projectId, @PathVariable UUID discoveryId) {
        DiscoveryRun run = discoveryService.executeDiscovery(projectId, discoveryId);
        return toResponse(run);
    }

    private DiscoveryResponse toResponse(DiscoveryRun run) {
        return new DiscoveryResponse(
                run.getId(),
                run.getProject().getId(),
                run.getProvider(),
                run.getStatus(),
                run.getAccountId(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getSummaryJson()
        );
    }

    private Integer extractProgress(String summaryJson) {
        if (summaryJson == null || summaryJson.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> summary = objectMapper.readValue(summaryJson, Map.class);
            Object progress = summary.get("progress");
            if (progress instanceof Number) {
                return ((Number) progress).intValue();
            }
            return null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
