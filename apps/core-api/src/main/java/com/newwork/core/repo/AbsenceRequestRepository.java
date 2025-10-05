package com.newwork.core.repo;

import com.newwork.core.domain.AbsenceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, UUID> {
    List<AbsenceRequest> findByEmployeeIdOrderByStartDateDesc(UUID employeeId);
}
