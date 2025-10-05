package com.newwork.core.service;

import com.newwork.core.domain.Employee;
import com.newwork.core.domain.Feedback;
import com.newwork.core.repo.EmployeeRepository;
import com.newwork.core.repo.FeedbackRepository;
import com.newwork.core.security.Role;
import com.newwork.core.security.UserPrincipal;
import com.newwork.core.service.impl.DefaultFeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultFeedbackServiceTest {

    EmployeeRepository employeeRepo;
    FeedbackRepository feedbackRepo;
    AiPolishService ai;
    FeedbackService service;

    UUID empId, authorEmpId;

    @BeforeEach
    void setup() {
        employeeRepo = mock(EmployeeRepository.class);
        feedbackRepo = mock(FeedbackRepository.class);
        ai = mock(AiPolishService.class);
        service = new DefaultFeedbackService(employeeRepo, feedbackRepo, ai);
        empId = UUID.randomUUID();
        authorEmpId = UUID.randomUUID();
        var e = new Employee(); e.setId(empId);
        when(employeeRepo.findById(empId)).thenReturn(Optional.of(e));
    }

    private static UserPrincipal principal(UUID userId, Role role, UUID employeeId) {
        return new UserPrincipal(userId, role, employeeId);
    }

    @Test
    void create_coworker_polishes_and_saves() {
        when(ai.polish("typo sentnce")).thenReturn("Typo sentence.");
        when(ai.modelId()).thenReturn("vennify/t5-base-grammar-correction");
        when(feedbackRepo.save(any())).thenAnswer(inv -> {
            Feedback f = inv.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });

        var out = service.createForEmployee(
                empId,
                new com.newwork.core.web.dto.FeedbackDtos.CreateFeedbackReq("typo sentnce"),
                principal(UUID.randomUUID(), Role.COWORKER, authorEmpId)
        );

        assertEquals("Typo sentence.", out.textPolished());
        assertEquals("vennify/t5-base-grammar-correction", out.polishModel());

        ArgumentCaptor<Feedback> cap = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepo).save(cap.capture());
        assertEquals("typo sentnce", cap.getValue().getTextOriginal());
        assertEquals(authorEmpId, cap.getValue().getAuthorEmployeeId());
    }

    @Test
    void create_employee_forbidden() {
        var p = principal(UUID.randomUUID(), Role.EMPLOYEE, authorEmpId);
        var ex = assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                service.createForEmployee(empId,
                        new com.newwork.core.web.dto.FeedbackDtos.CreateFeedbackReq("x"), p));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void list_owner_allowed() {
        when(feedbackRepo.findByEmployeeIdOrderByCreatedAtDesc(empId)).thenReturn(List.of());
        var p = principal(UUID.randomUUID(), Role.EMPLOYEE, empId);
        var out = service.listForEmployee(empId, p);
        assertNotNull(out);
    }

    @Test
    void list_manager_allowed() {
        when(feedbackRepo.findByEmployeeIdOrderByCreatedAtDesc(empId)).thenReturn(List.of());
        var p = principal(UUID.randomUUID(), Role.MANAGER, UUID.randomUUID());
        var out = service.listForEmployee(empId, p);
        assertNotNull(out);
    }

    @Test
    void list_coworker_forbidden() {
        var p = principal(UUID.randomUUID(), Role.COWORKER, authorEmpId);
        var ex = assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                service.listForEmployee(empId, p));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void create_missing_text_bad_request() {
        var p = principal(UUID.randomUUID(), Role.COWORKER, authorEmpId);
        var ex = assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                service.createForEmployee(empId,
                        new com.newwork.core.web.dto.FeedbackDtos.CreateFeedbackReq("  "), p));
        assertEquals(400, ex.getStatusCode().value());
    }
}
