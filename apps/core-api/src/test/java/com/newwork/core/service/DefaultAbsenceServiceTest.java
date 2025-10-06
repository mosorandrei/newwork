package com.newwork.core.service;

import com.newwork.core.domain.AbsenceRequest;
import com.newwork.core.domain.AbsenceStatus;
import com.newwork.core.domain.AbsenceType;
import com.newwork.core.domain.Employee;
import com.newwork.core.repo.AbsenceRequestRepository;
import com.newwork.core.repo.EmployeeRepository;
import com.newwork.core.security.Role;
import com.newwork.core.security.UserPrincipal;
import com.newwork.core.service.impl.DefaultAbsenceService;
import com.newwork.core.web.dto.AbsenceDtos;
import com.newwork.core.web.support.Etags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultAbsenceServiceTest {

    AbsenceRequestRepository absenceRepo;
    EmployeeRepository employeeRepo;
    DefaultAbsenceService service;

    final Etags etags = new Etags();

    @BeforeEach
    void setUp() {
        absenceRepo = mock(AbsenceRequestRepository.class);
        employeeRepo = mock(EmployeeRepository.class);
        service = new DefaultAbsenceService(employeeRepo, absenceRepo, etags);
    }

    private UserPrincipal manager() { return new UserPrincipal(UUID.randomUUID(), Role.MANAGER, UUID.randomUUID()); }
    private UserPrincipal owner(UUID empId) { return new UserPrincipal(UUID.randomUUID(), Role.EMPLOYEE, empId); }

    @Test
    void approve_missingIfMatch_throws428() {
        var a = samplePending(0);
        when(absenceRepo.findById(a.getId())).thenReturn(Optional.of(a));

        assertThrows(ResponseStatusException.class,
                () -> service.approve(a.getId(), new AbsenceDtos.DecisionReq("ok"), null, manager()));
    }

    @Test
    void approve_badIfMatch_throws412() {
        var a = samplePending(0);
        when(absenceRepo.findById(a.getId())).thenReturn(Optional.of(a));

        assertThrows(ResponseStatusException.class,
                () -> service.approve(a.getId(), new AbsenceDtos.DecisionReq("ok"), "not-a-number", manager()));
    }

    @Test
    void approve_versionMismatch_throws409() {
        var a = samplePending(2); // current version=2, If-Match "1" is stale
        when(absenceRepo.findById(a.getId())).thenReturn(Optional.of(a));

        assertThrows(Etags.VersionMismatchException.class,
                () -> service.approve(a.getId(), new AbsenceDtos.DecisionReq("ok"), "\"1\"", manager()));
    }

    @Test
    void cancel_owner_pending_ok() {
        var a = samplePending(0);
        when(absenceRepo.findById(a.getId())).thenReturn(Optional.of(a));
        when(absenceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var out = service.cancel(a.getId(), new AbsenceDtos.DecisionReq(null), "\"0\"", owner(a.getEmployee().getId()));
        assertEquals(AbsenceStatus.CANCELLED, out.status());
    }

    @Test
    void create_owner_valid_ok() {
        var emp = new Employee(); emp.setId(UUID.randomUUID());
        when(employeeRepo.findById(emp.getId())).thenReturn(Optional.of(emp));
        when(absenceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new AbsenceDtos.CreateAbsenceReq(
                LocalDate.parse("2025-10-20"), LocalDate.parse("2025-10-24"), AbsenceType.VACATION, "Trip");
        var out = service.create(emp.getId(), req, owner(emp.getId()));
        assertEquals(AbsenceStatus.PENDING, out.status());
    }

    private AbsenceRequest samplePending(int version) {
        var e = new Employee(); e.setId(UUID.randomUUID());
        var a = new AbsenceRequest();
        a.setEmployee(e);
        a.setType(AbsenceType.VACATION);
        a.setStartDate(LocalDate.parse("2025-10-20"));
        a.setEndDate(LocalDate.parse("2025-10-24"));
        a.setStatus(AbsenceStatus.PENDING);
        a.setReason("Trip");
        a.setVersion(version);
        try {
            var idF = AbsenceRequest.class.getDeclaredField("id");
            idF.setAccessible(true);
            idF.set(a, UUID.randomUUID());
        } catch (Exception ignored) {}
        return a;
    }
}
