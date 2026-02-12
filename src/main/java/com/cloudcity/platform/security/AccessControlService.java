package com.cloudcity.platform.security;

import com.cloudcity.platform.domain.OrgRole;
import com.cloudcity.platform.domain.Project;
import com.cloudcity.platform.repository.OrgMemberRepository;
import com.cloudcity.platform.repository.ProjectRepository;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessControlService {
    private final OrgMemberRepository orgMemberRepository;
    private final ProjectRepository projectRepository;

    public AccessControlService(OrgMemberRepository orgMemberRepository, ProjectRepository projectRepository) {
        this.orgMemberRepository = orgMemberRepository;
        this.projectRepository = projectRepository;
    }

    public void requireOrgRole(UUID orgId, Set<OrgRole> allowedRoles) {
        Optional<UUID> actorId = getActorId();
        if (actorId.isEmpty()) {
            return;
        }
        boolean allowed = orgMemberRepository.existsByIdOrgIdAndIdUserIdAndRoleIn(
                orgId, actorId.get(), allowedRoles
        );
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient org role");
        }
    }

    public void requireProjectRole(UUID projectId, Set<OrgRole> allowedRoles) {
        Optional<UUID> actorId = getActorId();
        if (actorId.isEmpty()) {
            return;
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        boolean allowed = orgMemberRepository.existsByIdOrgIdAndIdUserIdAndRoleIn(
                project.getOrg().getId(), actorId.get(), allowedRoles
        );
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient project role");
        }
    }

    public static Set<OrgRole> editorsAndAdmins() {
        return EnumSet.of(OrgRole.ADMIN, OrgRole.EDITOR);
    }

    public static Set<OrgRole> viewersAndAbove() {
        return EnumSet.of(OrgRole.ADMIN, OrgRole.EDITOR, OrgRole.VIEWER);
    }

    private Optional<UUID> getActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String principalString) {
            try {
                return Optional.of(UUID.fromString(principalString));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
