package com.cloudcity.platform.service;

import com.cloudcity.platform.api.dto.BudgetStatus;
import com.cloudcity.platform.api.dto.CostPolicyCheckRequest;
import com.cloudcity.platform.api.dto.CostPolicyCheckResponse;
import com.cloudcity.platform.domain.AuditEvent;
import com.cloudcity.platform.domain.CostSnapshot;
import com.cloudcity.platform.domain.Project;
import com.cloudcity.platform.domain.ResourceNode;
import com.cloudcity.platform.repository.AuditEventRepository;
import com.cloudcity.platform.repository.CostSnapshotRepository;
import com.cloudcity.platform.repository.ProjectRepository;
import com.cloudcity.platform.repository.ResourceNodeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public CostService(ProjectRepository projectRepository,
                       ResourceNodeRepository nodeRepository,
                       CostSnapshotRepository snapshotRepository,
                       AuditEventRepository auditEventRepository,
                       ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.nodeRepository = nodeRepository;
        this.snapshotRepository = snapshotRepository;
        this.auditEventRepository = auditEventRepository;
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

    @Transactional(readOnly = true)
    public BudgetEvaluation evaluateBudget(UUID projectId, BigDecimal total) {
        Project project = verifyProject(projectId);
        BigDecimal monthlyBudget = project.getMonthlyBudget();
        BigDecimal warningThreshold = project.getBudgetWarningThreshold() == null
                ? new BigDecimal("80.00")
                : project.getBudgetWarningThreshold();

        if (monthlyBudget == null || monthlyBudget.compareTo(BigDecimal.ZERO) <= 0) {
            return new BudgetEvaluation(BudgetStatus.NOT_CONFIGURED, null, null, warningThreshold);
        }

        BigDecimal usedPercent = total
                .multiply(new BigDecimal("100"))
                .divide(monthlyBudget, 2, RoundingMode.HALF_UP);

        if (usedPercent.compareTo(new BigDecimal("100.00")) >= 0) {
            return new BudgetEvaluation(BudgetStatus.EXCEEDED, monthlyBudget, usedPercent, warningThreshold);
        }
        if (usedPercent.compareTo(warningThreshold) >= 0) {
            return new BudgetEvaluation(BudgetStatus.WARNING, monthlyBudget, usedPercent, warningThreshold);
        }
        return new BudgetEvaluation(BudgetStatus.OK, monthlyBudget, usedPercent, warningThreshold);
    }

    @Transactional
    public CostPolicyCheckResponse policyCheck(UUID projectId, CostPolicyCheckRequest request) {
        BigDecimal currentTotal = computeCurrentTotal(projectId);
        BigDecimal projectedDelta = request == null || request.getProjectedMonthlyDelta() == null
                ? BigDecimal.ZERO
                : request.getProjectedMonthlyDelta();
        BigDecimal projectedTotal = currentTotal.add(projectedDelta);

        BudgetEvaluation evaluation = evaluateBudget(projectId, projectedTotal);
        boolean allowed = evaluation.status() != BudgetStatus.EXCEEDED;
        String reason = buildPolicyReason(evaluation.status(), evaluation.monthlyBudget(), projectedTotal);

        if (!allowed) {
            saveBudgetExceededAuditEvent(projectId, currentTotal, projectedTotal, projectedDelta, evaluation);
        }

        return new CostPolicyCheckResponse(
                allowed,
                evaluation.status(),
                evaluation.monthlyBudget(),
                currentTotal,
                projectedTotal,
                projectedDelta,
                evaluation.usedPercent(),
                evaluation.warningThreshold(),
                reason
        );
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

    private void saveBudgetExceededAuditEvent(UUID projectId,
                                              BigDecimal currentTotal,
                                              BigDecimal projectedTotal,
                                              BigDecimal projectedDelta,
                                              BudgetEvaluation evaluation) {
        AuditEvent event = new AuditEvent();
        event.setProject(projectRepository.getReferenceById(projectId));
        event.setActor(null);
        event.setAction("COST_POLICY_CHECK_FAILED");
        event.setEntityType("PROJECT");
        event.setEntityId(projectId);
        event.setChangesJson(serializePolicyFailure(currentTotal, projectedTotal, projectedDelta, evaluation));
        auditEventRepository.save(event);
    }

    private String serializePolicyFailure(BigDecimal currentTotal,
                                          BigDecimal projectedTotal,
                                          BigDecimal projectedDelta,
                                          BudgetEvaluation evaluation) {
        Map<String, Object> changes = new HashMap<>();
        changes.put("currentTotal", currentTotal);
        changes.put("projectedTotal", projectedTotal);
        changes.put("projectedDelta", projectedDelta);
        changes.put("monthlyBudget", evaluation.monthlyBudget());
        changes.put("budgetUsedPercent", evaluation.usedPercent());
        changes.put("budgetWarningThreshold", evaluation.warningThreshold());
        changes.put("budgetStatus", evaluation.status().name());
        try {
            return objectMapper.writeValueAsString(changes);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cost policy failure payload");
        }
    }

    private String buildPolicyReason(BudgetStatus status, BigDecimal monthlyBudget, BigDecimal projectedTotal) {
        if (status == BudgetStatus.NOT_CONFIGURED) {
            return "No monthly budget configured.";
        }
        if (status == BudgetStatus.OK) {
            return "Projected spend is within budget.";
        }
        if (status == BudgetStatus.WARNING) {
            return "Projected spend exceeds warning threshold.";
        }
        return "Projected spend " + projectedTotal + " exceeds monthly budget " + monthlyBudget + ".";
    }

    public record BudgetEvaluation(BudgetStatus status,
                                   BigDecimal monthlyBudget,
                                   BigDecimal usedPercent,
                                   BigDecimal warningThreshold) {
    }
}
