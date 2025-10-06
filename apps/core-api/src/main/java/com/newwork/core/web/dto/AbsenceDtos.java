package com.newwork.core.web.dto;

import com.newwork.core.domain.AbsenceStatus;
import com.newwork.core.domain.AbsenceType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class AbsenceDtos {
    public record CreateAbsenceReq(LocalDate startDate, LocalDate endDate, AbsenceType type, String reason) {}
    public record DecisionReq(String comment) {}
    public record AbsenceView(
            UUID id, UUID employeeId,
            AbsenceType type, LocalDate startDate, LocalDate endDate,
            String reason, AbsenceStatus status, String managerComment,
            Instant createdAt, Instant updatedAt, Integer version
    ) {}
}
