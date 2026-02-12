package com.cloudcity.platform.api;

import com.cloudcity.platform.api.dto.TerraformApprovalRequest;
import com.cloudcity.platform.api.dto.TerraformExportResponse;
import com.cloudcity.platform.domain.TerraformExport;
import com.cloudcity.platform.service.TerraformExportService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/projects/{projectId}/terraform")
@Tag(name = "Terraform")
public class TerraformExportController {
    private final TerraformExportService exportService;

    public TerraformExportController(TerraformExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/export")
    @ResponseStatus(HttpStatus.CREATED)
    public TerraformExportResponse createExport(@PathVariable UUID projectId) {
        TerraformExport export = exportService.createExport(projectId);
        return toResponse(export);
    }

    @GetMapping("/export/{exportId}")
    public TerraformExportResponse getExport(@PathVariable UUID projectId, @PathVariable UUID exportId) {
        TerraformExport export = exportService.getExport(projectId, exportId);
        return toResponse(export);
    }

    @PostMapping("/plan")
    @ResponseStatus(HttpStatus.CREATED)
    public TerraformExportResponse createPlan(@PathVariable UUID projectId) {
        TerraformExport export = exportService.plan(projectId);
        return toResponse(export);
    }

    @PostMapping("/{exportId}/approve")
    public TerraformExportResponse approvePlan(@PathVariable UUID projectId,
                                               @PathVariable UUID exportId,
                                               @Valid @RequestBody TerraformApprovalRequest request) {
        TerraformExport export = exportService.approve(projectId, exportId, request.isApproved(), request.getReason());
        return toResponse(export);
    }

    @PostMapping("/{exportId}/apply")
    public TerraformExportResponse applyPlan(@PathVariable UUID projectId, @PathVariable UUID exportId) {
        TerraformExport export = exportService.apply(projectId, exportId);
        return toResponse(export);
    }

    private TerraformExportResponse toResponse(TerraformExport export) {
        return new TerraformExportResponse(
                export.getId(),
                export.getProject().getId(),
                export.getStatus(),
                export.getSummaryJson(),
                export.getArtifactPath(),
                export.getCreatedAt()
        );
    }
}
