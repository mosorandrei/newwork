package com.newwork.core.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmployeeDtos {
    public record CreateEmployeeReq(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName
    ) {}

    public record UpdateEmployeeReq(
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName
    ) {}
}
