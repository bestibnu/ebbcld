package com.cloudcity.platform.repository;

import com.cloudcity.platform.domain.DiscoveryRun;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscoveryRunRepository extends JpaRepository<DiscoveryRun, UUID> {
    Optional<DiscoveryRun> findByIdAndProjectId(UUID id, UUID projectId);
}
