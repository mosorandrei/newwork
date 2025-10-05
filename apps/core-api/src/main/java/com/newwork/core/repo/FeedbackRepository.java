package com.newwork.core.repo;

import com.newwork.core.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
    List<Feedback> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);
}
