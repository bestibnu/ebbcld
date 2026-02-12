package com.cloudcity.platform.service;

import com.cloudcity.platform.api.dto.OrgCreateRequest;
import com.cloudcity.platform.api.dto.OrgMemberCreateRequest;
import com.cloudcity.platform.api.dto.UserCreateRequest;
import com.cloudcity.platform.domain.Org;
import com.cloudcity.platform.domain.OrgMember;
import com.cloudcity.platform.domain.OrgRole;
import com.cloudcity.platform.domain.User;
import com.cloudcity.platform.repository.OrgMemberRepository;
import com.cloudcity.platform.repository.OrgRepository;
import com.cloudcity.platform.repository.UserRepository;
import com.cloudcity.platform.security.AccessControlService;
import java.util.EnumSet;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrgService {
    private final OrgRepository orgRepository;
    private final UserRepository userRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final AccessControlService accessControlService;

    public OrgService(OrgRepository orgRepository,
                      UserRepository userRepository,
                      OrgMemberRepository orgMemberRepository,
                      AccessControlService accessControlService) {
        this.orgRepository = orgRepository;
        this.userRepository = userRepository;
        this.orgMemberRepository = orgMemberRepository;
        this.accessControlService = accessControlService;
    }

    @Transactional
    public Org createOrg(OrgCreateRequest request) {
        Org org = new Org();
        org.setName(request.getName());
        return orgRepository.save(org);
    }

    @Transactional
    public User createUser(UserCreateRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        return userRepository.save(user);
    }

    @Transactional
    public OrgMember addMember(UUID orgId, OrgMemberCreateRequest request) {
        accessControlService.requireOrgRole(orgId, EnumSet.of(OrgRole.ADMIN));
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Org not found"));
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (orgMemberRepository.existsByIdOrgIdAndIdUserId(org.getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already in org");
        }

        OrgRole role = request.getRole();
        OrgMember member = new OrgMember(org, user, role);
        return orgMemberRepository.save(member);
    }
}
