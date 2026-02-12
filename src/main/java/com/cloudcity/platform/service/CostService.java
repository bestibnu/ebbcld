package com.cloudcity.platform.service;

import com.cloudcity.platform.domain.CostSnapshot;
import com.cloudcity.platform.domain.Project;
import com.cloudcity.platform.domain.ResourceNode;
import com.cloudcity.platform.repository.CostSnapshotRepository;
import com.cloudcity.platform.repository.ProjectRepository;
import com.cloudcity.platform.repository.ResourceNodeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CostService {
    private static final String CURRENCY = "USD";
    private static final String PRICING_VERSION = "stub-1";

    private final ProjectRepository projectRepository;
    private final ResourceNodeRepository nodeRepository;
    private final CostSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    public CostService(ProjectRepository projectRepository,
                       ResourceNodeRepository nodeRepository,
                       CostSnapshotRepository snapshotRepository,
                       ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.nodeRepository = nodeRepository;
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public CostSnapshot getLatest(UUID projectId) {
        verifyProject(projectId);
        return snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId).orElse(null);
    }

    @Transactional
    public CostSnapshot recompute(UUID projectId) {
        Project project = verifyProject(projectId);
        List<ResourceNode> nodes = nodeRepository.findAllByProjectId(projectId);

        Map<String, BigDecimal> breakdown = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ResourceNode node : nodes) {
            BigDecimal cost = node.getCostEstimate() == null ? BigDecimal.ZERO : node.getCostEstimate();
            total = total.add(cost);
            String key = node.getType().name();
            breakdown.put(key, breakdown.getOrDefault(key, BigDecimal.ZERO).add(cost));
        }

        CostSnapshot snapshot = new CostSnapshot();
        snapshot.setProject(project);
        snapshot.setTotalCost(total);
        snapshot.setCurrency(CURRENCY);
        snapshot.setPricingVersion(PRICING_VERSION);
        snapshot.setBreakdownJson(serializeBreakdown(breakdown));
        return snapshotRepository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public BigDecimal computeCurrentTotal(UUID projectId) {
        verifyProject(projectId);
        List<ResourceNode> nodes = nodeRepository.findAllByProjectId(projectId);
        BigDecimal total = BigDecimal.ZERO;
        for (ResourceNode node : nodes) {
            BigDecimal cost = node.getCostEstimate() == null ? BigDecimal.ZERO : node.getCostEstimate();
            total = total.add(cost);
        }
        return total;
    }

    private Project verifyProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private String serializeBreakdown(Map<String, BigDecimal> breakdown) {
        try {
            return objectMapper.writeValueAsString(breakdown);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid breakdown");
        }
    }

    public String getCurrency() {
        return CURRENCY;
    }
}
