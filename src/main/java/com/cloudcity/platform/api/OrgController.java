package com.cloudcity.platform.api;

import com.cloudcity.platform.api.dto.OrgCreateRequest;
import com.cloudcity.platform.api.dto.OrgMemberCreateRequest;
import com.cloudcity.platform.api.dto.OrgMemberResponse;
import com.cloudcity.platform.api.dto.OrgResponse;
import com.cloudcity.platform.domain.Org;
import com.cloudcity.platform.domain.OrgMember;
import com.cloudcity.platform.service.OrgService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Orgs")
public class OrgController {
    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    @PostMapping("/orgs")
    @ResponseStatus(HttpStatus.CREATED)
    public OrgResponse createOrg(@Valid @RequestBody OrgCreateRequest request) {
        Org org = orgService.createOrg(request);
        return new OrgResponse(org.getId(), org.getName(), org.getCreatedAt());
    }

    @PostMapping("/orgs/{orgId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public OrgMemberResponse addMember(@PathVariable UUID orgId,
                                       @Valid @RequestBody OrgMemberCreateRequest request) {
        OrgMember member = orgService.addMember(orgId, request);
        return new OrgMemberResponse(
                member.getOrg().getId(),
                member.getUser().getId(),
                member.getRole(),
                member.getCreatedAt()
        );
    }
}
