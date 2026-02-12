package com.cloudcity.platform.api;

import com.cloudcity.platform.api.dto.ProjectCreateRequest;
import com.cloudcity.platform.api.dto.ProjectResponse;
import com.cloudcity.platform.api.dto.ProjectUpdateRequest;
import com.cloudcity.platform.domain.Project;
import com.cloudcity.platform.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/orgs/{orgId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@PathVariable UUID orgId, @Valid @RequestBody ProjectCreateRequest request) {
        Project project = projectService.createProject(orgId, request);
        return toProjectResponse(project);
    }

    @GetMapping("/projects/{projectId}")
    public ProjectResponse getProject(@PathVariable UUID projectId) {
        Project project = projectService.getProject(projectId);
        return toProjectResponse(project);
    }

    @PatchMapping("/projects/{projectId}")
    public ProjectResponse updateProject(@PathVariable UUID projectId, @RequestBody ProjectUpdateRequest request) {
        Project project = projectService.updateProject(projectId, request);
        return toProjectResponse(project);
    }

    private ProjectResponse toProjectResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getOrg().getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
