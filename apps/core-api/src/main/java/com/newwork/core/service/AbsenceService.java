package com.newwork.core.service;

import com.newwork.core.security.UserPrincipal;
import com.newwork.core.web.dto.AbsenceDtos.*;

import java.util.List;
import java.util.UUID;

public interface AbsenceService {
    List<AbsenceView> listForEmployee(UUID employeeId, UserPrincipal caller);
    AbsenceView create(UUID employeeId, CreateAbsenceReq req, UserPrincipal caller);
    AbsenceView getOne(UUID absenceId, UserPrincipal caller);
    AbsenceView approve(UUID absenceId, DecisionReq req, String ifMatch, UserPrincipal caller);
    AbsenceView reject(UUID absenceId, DecisionReq req, String ifMatch, UserPrincipal caller);
    AbsenceView cancel(UUID absenceId, DecisionReq req, String ifMatch, UserPrincipal caller);
}
