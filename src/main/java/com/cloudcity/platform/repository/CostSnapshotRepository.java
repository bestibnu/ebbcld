package com.cloudcity.platform.repository;

import com.cloudcity.platform.domain.CostSnapshot;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostSnapshotRepository extends JpaRepository<CostSnapshot, UUID> {
    Optional<CostSnapshot> findTopByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
