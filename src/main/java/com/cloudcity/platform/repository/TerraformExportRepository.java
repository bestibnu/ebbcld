package com.cloudcity.platform.repository;

import com.cloudcity.platform.domain.TerraformExport;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerraformExportRepository extends JpaRepository<TerraformExport, UUID> {
    Optional<TerraformExport> findByIdAndProjectId(UUID id, UUID projectId);
}
