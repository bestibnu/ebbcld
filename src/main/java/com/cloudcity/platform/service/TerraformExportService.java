package com.cloudcity.platform.service;

import com.cloudcity.platform.api.dto.BudgetStatus;
import com.cloudcity.platform.domain.AuditEvent;
import com.cloudcity.platform.domain.Project;
import com.cloudcity.platform.domain.ResourceNode;
import com.cloudcity.platform.domain.ResourceType;
import com.cloudcity.platform.domain.TerraformExport;
import com.cloudcity.platform.repository.AuditEventRepository;
import com.cloudcity.platform.repository.ProjectRepository;
import com.cloudcity.platform.repository.ResourceNodeRepository;
import com.cloudcity.platform.repository.TerraformExportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TerraformExportService {
    private static final String STATUS_READY = "READY";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_APPLYING = "APPLYING";
    private static final String STATUS_APPLIED = "APPLIED";

    private final ProjectRepository projectRepository;
    private final ResourceNodeRepository nodeRepository;
    private final TerraformExportRepository exportRepository;
    private final AuditEventRepository auditEventRepository;
    private final CostService costService;
    private final ObjectMapper objectMapper;
    private final String exportDir;

    public TerraformExportService(ProjectRepository projectRepository,
                                  ResourceNodeRepository nodeRepository,
                                  TerraformExportRepository exportRepository,
                                  AuditEventRepository auditEventRepository,
                                  CostService costService,
                                  ObjectMapper objectMapper,
                                  @Value("${cloudcity.terraform.export-dir}") String exportDir) {
        this.projectRepository = projectRepository;
        this.nodeRepository = nodeRepository;
        this.exportRepository = exportRepository;
        this.auditEventRepository = auditEventRepository;
        this.costService = costService;
        this.objectMapper = objectMapper;
        this.exportDir = exportDir;
    }

    @Transactional
    public TerraformExport createExport(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        List<ResourceNode> nodes = nodeRepository.findAllByProjectId(projectId);
        ExportPayload payload = buildPayload(nodes);
        validatePayload(payload);

        TerraformExport export = new TerraformExport();
        export.setProject(project);
        export.setStatus(STATUS_READY);
        export.setSummaryJson(serializeSummary(payload));

        try {
            Path exportPath = writeExportFiles(projectId, payload);
            export.setArtifactPath(exportPath.toString());
            return exportRepository.save(export);
        } catch (IOException e) {
            export.setStatus(STATUS_FAILED);
            export.setSummaryJson("{\"error\":\"Failed to write export\"}");
            return exportRepository.save(export);
        }
    }

    @Transactional(readOnly = true)
    public TerraformExport getExport(UUID projectId, UUID exportId) {
        return exportRepository.findByIdAndProjectId(exportId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Export not found"));
    }

    @Transactional
    public TerraformExport plan(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        List<ResourceNode> nodes = nodeRepository.findAllByProjectId(projectId);
        ExportPayload payload = buildPayload(nodes);
        validatePayload(payload);

        TerraformExport export = new TerraformExport();
        export.setProject(project);

        try {
            Path exportPath = writeExportFiles(projectId, payload);
            export.setArtifactPath(exportPath.toString());
        } catch (IOException e) {
            export.setStatus(STATUS_FAILED);
            export.setSummaryJson("{\"error\":\"Failed to write plan artifact\"}");
            return exportRepository.save(export);
        }

        CostService.BudgetEvaluation evaluation = costService.evaluateBudget(projectId, costService.computeCurrentTotal(projectId));
        String nextStatus = evaluation.status() == BudgetStatus.EXCEEDED ? STATUS_REJECTED : STATUS_PENDING_APPROVAL;
        export.setStatus(nextStatus);
        export.setSummaryJson(buildPlanSummary(payload, projectId, evaluation));
        TerraformExport saved = exportRepository.save(export);

        if (STATUS_REJECTED.equals(nextStatus)) {
            createAuditEvent(projectId, "TERRAFORM_PLAN_BLOCKED", saved.getId(), saved.getSummaryJson());
        } else {
            createAuditEvent(projectId, "TERRAFORM_PLAN_CREATED", saved.getId(), saved.getSummaryJson());
        }
        return saved;
    }

    @Transactional
    public TerraformExport approve(UUID projectId, UUID exportId, boolean approved, String reason) {
        TerraformExport export = getExport(projectId, exportId);
        if (!STATUS_PENDING_APPROVAL.equals(export.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING_APPROVAL plans can be approved or rejected");
        }

        export.setStatus(approved ? STATUS_APPROVED : STATUS_REJECTED);
        export.setSummaryJson(updateSummaryWithApproval(export.getSummaryJson(), approved, reason));
        TerraformExport saved = exportRepository.save(export);

        createAuditEvent(
                projectId,
                approved ? "TERRAFORM_PLAN_APPROVED" : "TERRAFORM_PLAN_REJECTED",
                exportId,
                saved.getSummaryJson()
        );
        return saved;
    }

    @Transactional
    public TerraformExport apply(UUID projectId, UUID exportId) {
        TerraformExport export = getExport(projectId, exportId);
        if (!STATUS_APPROVED.equals(export.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only APPROVED plans can be applied");
        }

        CostService.BudgetEvaluation evaluation = costService.evaluateBudget(projectId, costService.computeCurrentTotal(projectId));
        if (evaluation.status() == BudgetStatus.EXCEEDED) {
            export.setStatus(STATUS_FAILED);
            export.setSummaryJson(updateSummaryWithApplyFailure(export.getSummaryJson(), "Budget policy check failed at apply"));
            TerraformExport failed = exportRepository.save(export);
            createAuditEvent(projectId, "TERRAFORM_APPLY_BLOCKED", exportId, failed.getSummaryJson());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Budget policy check failed");
        }

        export.setStatus(STATUS_APPLYING);
        exportRepository.save(export);

        export.setStatus(STATUS_APPLIED);
        export.setSummaryJson(updateSummaryWithApplySuccess(export.getSummaryJson()));
        TerraformExport applied = exportRepository.save(export);
        createAuditEvent(projectId, "TERRAFORM_APPLIED", exportId, applied.getSummaryJson());
        return applied;
    }

    private ExportPayload buildPayload(List<ResourceNode> nodes) {
        List<ResourceNode> vpcs = new ArrayList<>();
        List<ResourceNode> subnets = new ArrayList<>();
        List<ResourceNode> sgs = new ArrayList<>();
        List<ResourceNode> ec2 = new ArrayList<>();
        List<ResourceNode> rds = new ArrayList<>();
        List<ResourceNode> elbs = new ArrayList<>();
        List<ResourceNode> s3 = new ArrayList<>();

        for (ResourceNode node : nodes) {
            ResourceType type = node.getType();
            if (type == ResourceType.VPC) {
                vpcs.add(node);
            } else if (type == ResourceType.SUBNET) {
                subnets.add(node);
            } else if (type == ResourceType.SG) {
                sgs.add(node);
            } else if (type == ResourceType.EC2) {
                ec2.add(node);
            } else if (type == ResourceType.RDS) {
                rds.add(node);
            } else if (type == ResourceType.ELB) {
                elbs.add(node);
            } else if (type == ResourceType.S3) {
                s3.add(node);
            }
        }

        return new ExportPayload(vpcs, subnets, sgs, ec2, rds, elbs, s3);
    }

    private String serializeSummary(ExportPayload payload) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("vpc", payload.vpcs.size());
        summary.put("subnet", payload.subnets.size());
        summary.put("sg", payload.sgs.size());
        summary.put("ec2", payload.ec2.size());
        summary.put("rds", payload.rds.size());
        summary.put("elb", payload.elbs.size());
        summary.put("s3", payload.s3.size());
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String buildPlanSummary(ExportPayload payload, UUID projectId, CostService.BudgetEvaluation evaluation) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("adds", payload.vpcs.size() + payload.subnets.size() + payload.sgs.size()
                + payload.ec2.size() + payload.rds.size() + payload.elbs.size() + payload.s3.size());
        summary.put("changes", 0);
        summary.put("destroys", 0);
        summary.put("estimatedMonthlyDelta", computeEstimatedMonthlyDelta(projectId));
        summary.put("budgetStatus", evaluation.status().name());
        summary.put("monthlyBudget", evaluation.monthlyBudget());
        summary.put("budgetUsedPercent", evaluation.usedPercent());
        summary.put("budgetWarningThreshold", evaluation.warningThreshold());
        summary.put("resourceCounts", parseMap(serializeSummary(payload)));
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> parseMap(String json) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            return new HashMap<>(parsed);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    private java.math.BigDecimal computeEstimatedMonthlyDelta(UUID projectId) {
        var latest = costService.getLatest(projectId);
        java.math.BigDecimal previous = latest == null ? java.math.BigDecimal.ZERO : latest.getTotalCost();
        java.math.BigDecimal current = costService.computeCurrentTotal(projectId);
        return current.subtract(previous);
    }

    private String updateSummaryWithApproval(String summaryJson, boolean approved, String reason) {
        Map<String, Object> summary = parseMap(summaryJson == null ? "{}" : summaryJson);
        summary.put("approval", approved ? "APPROVED" : "REJECTED");
        summary.put("approvalReason", reason);
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            return summaryJson;
        }
    }

    private String updateSummaryWithApplyFailure(String summaryJson, String error) {
        Map<String, Object> summary = parseMap(summaryJson == null ? "{}" : summaryJson);
        summary.put("applyStatus", "FAILED");
        summary.put("applyError", error);
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            return summaryJson;
        }
    }

    private String updateSummaryWithApplySuccess(String summaryJson) {
        Map<String, Object> summary = parseMap(summaryJson == null ? "{}" : summaryJson);
        summary.put("applyStatus", "APPLIED");
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            return summaryJson;
        }
    }

    private Path writeExportFiles(UUID projectId, ExportPayload payload) throws IOException {
        Path baseDir = Path.of(exportDir, projectId.toString(), "terraform");
        Files.createDirectories(baseDir);
        Path mainTf = baseDir.resolve("main.tf");
        Files.writeString(mainTf, buildMainTf(payload), StandardCharsets.UTF_8);
        return baseDir;
    }

    private String buildMainTf(ExportPayload payload) {
        StringBuilder sb = new StringBuilder();
        sb.append("terraform {\n");
        sb.append("  required_providers {\n");
        sb.append("    aws = {\n");
        sb.append("      source = \"hashicorp/aws\"\n");
        sb.append("      version = \"~> 5.0\"\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}\n\n");

        int i = 1;
        for (ResourceNode vpc : payload.vpcs) {
            sb.append("resource \"aws_vpc\" \"vpc_").append(i).append("\" {\n");
            sb.append("  cidr_block = \"10.").append(i).append(".0.0/16\"\n");
            sb.append("  tags = { Name = \"").append(escape(vpc.getName())).append("\" }\n");
            sb.append("}\n\n");
            i++;
        }

        int s = 1;
        for (ResourceNode subnet : payload.subnets) {
            sb.append("resource \"aws_subnet\" \"subnet_").append(s).append("\" {\n");
            sb.append("  vpc_id     = aws_vpc.vpc_1.id\n");
            sb.append("  cidr_block = \"10.1.").append(s).append(".0/24\"\n");
            sb.append("  tags = { Name = \"").append(escape(subnet.getName())).append("\" }\n");
            sb.append("}\n\n");
            s++;
        }

        int sg = 1;
        for (ResourceNode sgNode : payload.sgs) {
            sb.append("resource \"aws_security_group\" \"sg_").append(sg).append("\" {\n");
            sb.append("  name   = \"").append(escape(sgNode.getName())).append("\"\n");
            sb.append("  vpc_id = aws_vpc.vpc_1.id\n");
            sb.append("}\n\n");
            sg++;
        }

        int e = 1;
        for (ResourceNode ec2 : payload.ec2) {
            sb.append("resource \"aws_instance\" \"ec2_").append(e).append("\" {\n");
            sb.append("  ami           = \"ami-1234567890abcdef0\"\n");
            sb.append("  instance_type = \"t3.micro\"\n");
            sb.append("  tags = { Name = \"").append(escape(ec2.getName())).append("\" }\n");
            sb.append("}\n\n");
            e++;
        }

        int r = 1;
        for (ResourceNode rds : payload.rds) {
            sb.append("resource \"aws_db_instance\" \"rds_").append(r).append("\" {\n");
            sb.append("  allocated_storage = 20\n");
            sb.append("  engine            = \"postgres\"\n");
            sb.append("  instance_class    = \"db.t3.micro\"\n");
            sb.append("  username          = \"admin\"\n");
            sb.append("  password          = \"changeme123!\"\n");
            sb.append("  skip_final_snapshot = true\n");
            sb.append("}\n\n");
            r++;
        }

        int l = 1;
        for (ResourceNode elb : payload.elbs) {
            sb.append("resource \"aws_lb\" \"elb_").append(l).append("\" {\n");
            sb.append("  name               = \"").append(escape(elb.getName())).append("\"\n");
            sb.append("  internal           = false\n");
            sb.append("  load_balancer_type = \"application\"\n");
            sb.append("  subnets            = [aws_subnet.subnet_1.id]\n");
            sb.append("}\n\n");
            l++;
        }

        int b = 1;
        for (ResourceNode s3 : payload.s3) {
            sb.append("resource \"aws_s3_bucket\" \"s3_").append(b).append("\" {\n");
            sb.append("  bucket = \"").append(escape(s3.getName())).append("-bucket\"\n");
            sb.append("}\n\n");
            b++;
        }

        return sb.toString();
    }

    private void validatePayload(ExportPayload payload) {
        if (payload.vpcs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Terraform export requires at least one VPC");
        }
        if (!payload.elbs.isEmpty() && payload.subnets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Load balancers require at least one subnet");
        }
    }

    private void createAuditEvent(UUID projectId, String action, UUID entityId, String changesJson) {
        AuditEvent event = new AuditEvent();
        event.setProject(projectRepository.getReferenceById(projectId));
        event.setAction(action);
        event.setEntityType("TERRAFORM_EXPORT");
        event.setEntityId(entityId);
        event.setChangesJson(changesJson);
        auditEventRepository.save(event);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "");
    }

    private static class ExportPayload {
        private final List<ResourceNode> vpcs;
        private final List<ResourceNode> subnets;
        private final List<ResourceNode> sgs;
        private final List<ResourceNode> ec2;
        private final List<ResourceNode> rds;
        private final List<ResourceNode> elbs;
        private final List<ResourceNode> s3;

        private ExportPayload(List<ResourceNode> vpcs,
                              List<ResourceNode> subnets,
                              List<ResourceNode> sgs,
                              List<ResourceNode> ec2,
                              List<ResourceNode> rds,
                              List<ResourceNode> elbs,
                              List<ResourceNode> s3) {
            this.vpcs = vpcs;
            this.subnets = subnets;
            this.sgs = sgs;
            this.ec2 = ec2;
            this.rds = rds;
            this.elbs = elbs;
            this.s3 = s3;
        }
    }
}
