package com.cloudcity.platform.repository;

import com.cloudcity.platform.domain.ResourceNode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceNodeRepository extends JpaRepository<ResourceNode, UUID> {
    List<ResourceNode> findAllByProjectId(UUID projectId);

    Optional<ResourceNode> findByIdAndProjectId(UUID id, UUID projectId);
}
