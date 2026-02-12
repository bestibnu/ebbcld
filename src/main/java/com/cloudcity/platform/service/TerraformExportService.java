package com.cloudcity.platform.service;

import com.cloudcity.platform.domain.Project;
import com.cloudcity.platform.domain.ResourceNode;
import com.cloudcity.platform.domain.ResourceType;
import com.cloudcity.platform.domain.TerraformExport;
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

    private final ProjectRepository projectRepository;
    private final ResourceNodeRepository nodeRepository;
    private final TerraformExportRepository exportRepository;
    private final ObjectMapper objectMapper;
    private final String exportDir;

    public TerraformExportService(ProjectRepository projectRepository,
                                  ResourceNodeRepository nodeRepository,
                                  TerraformExportRepository exportRepository,
                                  ObjectMapper objectMapper,
                                  @Value("${cloudcity.terraform.export-dir}") String exportDir) {
        this.projectRepository = projectRepository;
        this.nodeRepository = nodeRepository;
        this.exportRepository = exportRepository;
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
