package com.cloudcity.platform.repository;

import com.cloudcity.platform.domain.Org;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgRepository extends JpaRepository<Org, UUID> {
}
