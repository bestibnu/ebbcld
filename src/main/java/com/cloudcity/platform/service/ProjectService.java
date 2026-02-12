package com.cloudcity.platform.service;

import com.cloudcity.platform.api.dto.ProjectCreateRequest;
import com.cloudcity.platform.api.dto.ProjectUpdateRequest;
import com.cloudcity.platform.domain.Org;
import com.cloudcity.platform.domain.Project;
import com.cloudcity.platform.repository.OrgRepository;
import com.cloudcity.platform.repository.ProjectRepository;
import com.cloudcity.platform.security.AccessControlService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectService {
    private final OrgRepository orgRepository;
    private final ProjectRepository projectRepository;
    private final AccessControlService accessControlService;

    public ProjectService(OrgRepository orgRepository,
                          ProjectRepository projectRepository,
                          AccessControlService accessControlService) {
        this.orgRepository = orgRepository;
        this.projectRepository = projectRepository;
        this.accessControlService = accessControlService;
    }

    @Transactional
    public Project createProject(UUID orgId, ProjectCreateRequest request) {
        accessControlService.requireOrgRole(orgId, AccessControlService.editorsAndAdmins());
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Org not found"));
        Project project = new Project();
        project.setOrg(org);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setMonthlyBudget(request.getMonthlyBudget());
        project.setBudgetWarningThreshold(request.getBudgetWarningThreshold());
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public Project getProject(UUID projectId) {
        accessControlService.requireProjectRole(projectId, AccessControlService.viewersAndAbove());
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    @Transactional
    public Project updateProject(UUID projectId, ProjectUpdateRequest request) {
        accessControlService.requireProjectRole(projectId, AccessControlService.editorsAndAdmins());
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getMonthlyBudget() != null) {
            project.setMonthlyBudget(request.getMonthlyBudget());
        }
        if (request.getBudgetWarningThreshold() != null) {
            project.setBudgetWarningThreshold(request.getBudgetWarningThreshold());
        }
        return projectRepository.save(project);
    }
}
