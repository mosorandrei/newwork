package com.newwork.core.service.impl;

import com.newwork.core.domain.AbsenceRequest;
import com.newwork.core.domain.AbsenceStatus;
import com.newwork.core.repo.AbsenceRequestRepository;
import com.newwork.core.repo.EmployeeRepository;
import com.newwork.core.security.Access;
import com.newwork.core.security.Role;
import com.newwork.core.security.UserPrincipal;
import com.newwork.core.service.AbsenceService;
import com.newwork.core.web.dto.AbsenceDtos.*;
import com.newwork.core.web.support.Etags;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class DefaultAbsenceService implements AbsenceService {

    private final EmployeeRepository employeeRepo;
    private final AbsenceRequestRepository absenceRepo;
    private final Etags etags;

    public DefaultAbsenceService(EmployeeRepository employeeRepo, AbsenceRequestRepository absenceRepo, Etags etags) {
        this.employeeRepo = employeeRepo;
        this.absenceRepo = absenceRepo;
        this.etags = etags;
    }

    @Override
    public List<AbsenceView> listForEmployee(UUID employeeId, UserPrincipal caller) {
        Access.requireOwnerOrManager(caller, employeeId);
        return absenceRepo.findByEmployeeIdOrderByStartDateDesc(employeeId)
                .stream().map(this::toView).toList();
    }

    @Override
    public AbsenceView create(UUID employeeId, CreateAbsenceReq req, UserPrincipal caller) {
        Access.requireOwner(caller, employeeId);

        if (req == null || req.type() == null || req.startDate() == null || req.endDate() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_request");
        if (req.startDate().isAfter(req.endDate()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date_range");

        var emp = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var a = new AbsenceRequest();
        a.setEmployee(emp);
        a.setType(req.type());
        a.setStartDate(req.startDate());
        a.setEndDate(req.endDate());
        a.setReason(req.reason());
        a.setStatus(AbsenceStatus.PENDING);

        return toView(absenceRepo.save(a));
    }

    @Override
    public AbsenceView getOne(UUID id, UserPrincipal caller) {
        Access.requireAuth(caller);
        var a = absenceRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean allowed = caller.role() == Role.MANAGER || Access.isOwner(caller, a.getEmployee().getId());
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return toView(a);
    }

    @Override
    public AbsenceView approve(UUID id, DecisionReq req, String ifMatch, UserPrincipal caller) {
        Access.requireManager(caller);
        var a = mustBePendingAndMatch(id, ifMatch);
        a.setStatus(AbsenceStatus.APPROVED);
        a.setManagerComment(req == null ? null : req.comment());
        return toView(absenceRepo.save(a));
    }

    @Override
    public AbsenceView reject(UUID id, DecisionReq req, String ifMatch, UserPrincipal caller) {
        Access.requireManager(caller);
        var a = mustBePendingAndMatch(id, ifMatch);
        a.setStatus(AbsenceStatus.REJECTED);
        a.setManagerComment(req == null ? null : req.comment());
        return toView(absenceRepo.save(a));
    }

    @Override
    public AbsenceView cancel(UUID id, DecisionReq req, String ifMatch, UserPrincipal caller) {
        Access.requireAuth(caller);
        var a = mustBePendingAndMatch(id, ifMatch);
        Access.requireOwner(caller, a.getEmployee().getId());
        a.setStatus(AbsenceStatus.CANCELLED);
        a.setManagerComment(req == null ? null : req.comment());
        return toView(absenceRepo.save(a));
    }

    private AbsenceRequest mustBePendingAndMatch(UUID id, String ifMatch) {
        var a = absenceRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        etags.assertMatches(a.getVersion(), ifMatch);
        if (a.getStatus() != AbsenceStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "not_pending");
        return a;
    }

    private AbsenceView toView(AbsenceRequest a) {
        return new AbsenceView(
                a.getId(),
                a.getEmployee().getId(),
                a.getType(),
                a.getStartDate(),
                a.getEndDate(),
                a.getReason(),
                a.getStatus(),
                a.getManagerComment(),
                a.getCreatedAt(),
                a.getUpdatedAt(),
                a.getVersion()
        );
    }
}
