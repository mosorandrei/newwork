package com.newwork.core.repo;

import com.newwork.core.domain.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, UUID> {
    Optional<EmployeeProfile> findByEmployeeId(UUID id);
}
