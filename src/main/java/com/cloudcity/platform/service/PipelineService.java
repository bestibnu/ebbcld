package com.cloudcity.platform.service;

import com.cloudcity.platform.api.dto.BudgetStatus;
import com.cloudcity.platform.api.dto.PipelineCheckRequest;
import com.cloudcity.platform.api.dto.PipelineCheckResponse;
import com.cloudcity.platform.domain.AuditEvent;
import com.cloudcity.platform.domain.ResourceNode;
import com.cloudcity.platform.domain.ResourceType;
import com.cloudcity.platform.repository.AuditEventRepository;
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
public class PipelineService {
    private final ProjectRepository projectRepository;
    private final ResourceNodeRepository resourceNodeRepository;
    private final CostService costService;
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public PipelineService(ProjectRepository projectRepository,
                           ResourceNodeRepository resourceNodeRepository,
                           CostService costService,
                           AuditEventRepository auditEventRepository,
                           ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.resourceNodeRepository = resourceNodeRepository;
        this.costService = costService;
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PipelineCheckResponse check(UUID projectId, PipelineCheckRequest request) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        BigDecimal projectedDelta = request == null || request.getProjectedMonthlyDelta() == null
                ? BigDecimal.ZERO
                : request.getProjectedMonthlyDelta();
        boolean strictMode = request != null && Boolean.TRUE.equals(request.getStrictMode());

        BigDecimal currentTotal = costService.computeCurrentTotal(projectId);
        BigDecimal projectedTotal = currentTotal.add(projectedDelta);
        CostService.BudgetEvaluation budget = costService.evaluateBudget(projectId, projectedTotal);

        TerraformEligibility eligibility = evaluateTerraformPlanEligibility(projectId);
        PipelineDecision decision = decide(budget.status(), strictMode, eligibility.eligible());

        PipelineCheckResponse response = new PipelineCheckResponse(
                decision.pass(),
                budget.status(),
                decision.requiredApproval(),
                eligibility.eligible(),
                decision.reason(eligibility.reason()),
                decision.recommendedAction(eligibility.recommendedAction()),
                currentTotal,
                projectedTotal,
                budget.monthlyBudget(),
                budget.usedPercent(),
                budget.warningThreshold(),
                strictMode
        );
        saveAuditEvent(projectId, response);
        return response;
    }

    private TerraformEligibility evaluateTerraformPlanEligibility(UUID projectId) {
        List<ResourceNode> nodes = resourceNodeRepository.findAllByProjectId(projectId);
        boolean hasVpc = nodes.stream().anyMatch(node -> node.getType() == ResourceType.VPC);
        boolean hasElb = nodes.stream().anyMatch(node -> node.getType() == ResourceType.ELB);
        boolean hasSubnet = nodes.stream().anyMatch(node -> node.getType() == ResourceType.SUBNET);

        if (!hasVpc) {
            return new TerraformEligibility(false, "Terraform plan requires at least one VPC",
                    "Add a VPC resource before running pipeline apply.");
        }
        if (hasElb && !hasSubnet) {
            return new TerraformEligibility(false, "Terraform plan has ELB resources but no subnet",
                    "Add at least one subnet for load balancer resources.");
        }
        return new TerraformEligibility(true, "Terraform plan is eligible", "Proceed to plan/approval/apply.");
    }

    private PipelineDecision decide(BudgetStatus budgetStatus, boolean strictMode, boolean terraformEligible) {
        if (!terraformEligible) {
            return new PipelineDecision(false, false,
                    "Terraform plan eligibility check failed", "Fix topology and rerun pipeline check.");
        }
        if (budgetStatus == BudgetStatus.EXCEEDED) {
            return new PipelineDecision(false, true,
                    "Projected spend exceeds monthly budget", "Reduce cost or increase budget before apply.");
        }
        if (budgetStatus == BudgetStatus.WARNING) {
            if (strictMode) {
                return new PipelineDecision(false, true,
                        "Strict mode blocks WARNING budget status", "Lower projected delta or disable strict mode.");
            }
            return new PipelineDecision(true, true,
                    "Projected spend exceeds warning threshold", "Require manual approval before apply.");
        }
        if (budgetStatus == BudgetStatus.NOT_CONFIGURED) {
            return new PipelineDecision(true, true,
                    "No monthly budget configured", "Set project budget to enforce hard guardrails.");
        }
        return new PipelineDecision(true, false, "Checks passed", "Continue pipeline.");
    }

    private void saveAuditEvent(UUID projectId, PipelineCheckResponse response) {
        AuditEvent event = new AuditEvent();
        event.setProject(projectRepository.getReferenceById(projectId));
        event.setAction("PIPELINE_CHECK_EXECUTED");
        event.setEntityType("PROJECT");
        event.setEntityId(projectId);
        event.setChangesJson(serializeResponse(response));
        auditEventRepository.save(event);
    }

    private String serializeResponse(PipelineCheckResponse response) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("pass", response.isPass());
        payload.put("budgetStatus", response.getBudgetStatus().name());
        payload.put("requiredApproval", response.isRequiredApproval());
        payload.put("terraformPlanEligible", response.isTerraformPlanEligible());
        payload.put("reason", response.getReason());
        payload.put("recommendedAction", response.getRecommendedAction());
        payload.put("currentTotal", response.getCurrentTotal());
        payload.put("projectedTotal", response.getProjectedTotal());
        payload.put("monthlyBudget", response.getMonthlyBudget());
        payload.put("budgetUsedPercent", response.getBudgetUsedPercent());
        payload.put("budgetWarningThreshold", response.getBudgetWarningThreshold());
        payload.put("strictMode", response.isStrictMode());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pipeline payload");
        }
    }

    private record TerraformEligibility(boolean eligible, String reason, String recommendedAction) {
    }

    private record PipelineDecision(boolean pass,
                                    boolean requiredApproval,
                                    String baseReason,
                                    String baseAction) {
        private String reason(String eligibilityReason) {
            if (pass && "Checks passed".equals(baseReason)) {
                return eligibilityReason;
            }
            return baseReason;
        }

        private String recommendedAction(String eligibilityAction) {
            if (pass && "Continue pipeline.".equals(baseAction)) {
                return eligibilityAction;
            }
            return baseAction;
        }
    }
}
