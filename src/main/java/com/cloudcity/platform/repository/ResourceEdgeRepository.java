package com.cloudcity.platform.repository;

import com.cloudcity.platform.domain.ResourceEdge;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceEdgeRepository extends JpaRepository<ResourceEdge, UUID> {
    List<ResourceEdge> findAllByProjectId(UUID projectId);

    Optional<ResourceEdge> findByIdAndProjectId(UUID id, UUID projectId);
}
