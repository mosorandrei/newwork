package com.newwork.core.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ProfileDtos {
    public record ProfileView(
            UUID employeeId,
            String bio,
            String skillsJson,
            BigDecimal salary,  // null for coworker
            String ssnMasked,   // masked (****1234) for manager/owner, null for coworker
            String address,     // null for coworker
            String contactEmail,
            Integer version
    ) {}
    public record UpdateProfileReq(
            String bio,
            String skillsJson,
            BigDecimal salary,
            String ssn,
            String address,
            String contactEmail
    ) {}
}
