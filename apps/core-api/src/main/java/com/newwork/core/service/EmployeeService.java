package com.newwork.core.service;

import com.newwork.core.domain.Employee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeService {
    List<Employee> getAllEmployees();

    Optional<Employee> findById(UUID id);

    Employee save(Employee employee);

    void delete(Employee employee);
}
