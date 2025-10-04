package com.newwork.core.repo;

import com.newwork.core.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
}
