package com.cloudcity.platform.repository;

import com.cloudcity.platform.domain.OrgMember;
import com.cloudcity.platform.domain.OrgMemberId;
import com.cloudcity.platform.domain.OrgRole;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgMemberRepository extends JpaRepository<OrgMember, OrgMemberId> {
    boolean existsByIdOrgIdAndIdUserId(UUID orgId, UUID userId);

    boolean existsByIdOrgIdAndIdUserIdAndRoleIn(UUID orgId, UUID userId, Collection<OrgRole> roles);
}
