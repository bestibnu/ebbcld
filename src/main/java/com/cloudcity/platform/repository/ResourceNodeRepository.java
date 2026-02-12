package com.cloudcity.platform.repository;

import com.cloudcity.platform.domain.ResourceNode;
import com.cloudcity.platform.domain.CloudProvider;
import com.cloudcity.platform.domain.ResourceSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceNodeRepository extends JpaRepository<ResourceNode, UUID> {
    List<ResourceNode> findAllByProjectId(UUID projectId);

    Optional<ResourceNode> findByIdAndProjectId(UUID id, UUID projectId);

    List<ResourceNode> findAllByProjectIdAndProviderAndRegionAndSource(
            UUID projectId,
            CloudProvider provider,
            String region,
            ResourceSource source
    );
}
