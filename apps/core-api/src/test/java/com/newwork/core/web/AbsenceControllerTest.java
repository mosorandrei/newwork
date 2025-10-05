package com.newwork.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newwork.core.domain.AbsenceStatus;
import com.newwork.core.domain.AbsenceType;
import com.newwork.core.security.JwtAuthFilter;
import com.newwork.core.security.JwtUtil;
import com.newwork.core.service.AbsenceService;
import com.newwork.core.web.dto.AbsenceDtos.AbsenceView;
import com.newwork.core.web.dto.AbsenceDtos.CreateAbsenceReq;
import com.newwork.core.web.dto.AbsenceDtos.DecisionReq;
import com.newwork.core.web.support.Etags;
import com.newwork.core.web.support.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AbsenceController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import({Etags.class, GlobalExceptionHandler.class})
class AbsenceControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean AbsenceService absenceService;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtUtil jwtUtil;

    private AbsenceView view(UUID id, UUID empId, int ver) {
        return new AbsenceView(
                id, empId,
                AbsenceType.VACATION,
                LocalDate.parse("2025-10-20"),
                LocalDate.parse("2025-10-24"),
                "Family trip",
                AbsenceStatus.PENDING,
                null,
                Instant.now(), Instant.now(), ver
        );
    }

    @Test
    void list_returns200_andArray() throws Exception {
        UUID empId = UUID.randomUUID();
        when(absenceService.listForEmployee(eq(empId), any()))
                .thenReturn(List.of(view(UUID.randomUUID(), empId, 0)));

        mvc.perform(get("/api/employees/{id}/absences", empId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value(empId.toString()));
    }

    @Test
    void create_returns201_withLocation_andEtag() throws Exception {
        UUID empId = UUID.randomUUID();
        UUID absId = UUID.randomUUID();
        when(absenceService.create(eq(empId), any(CreateAbsenceReq.class), any()))
                .thenReturn(view(absId, empId, 0));

        String body = "{\"startDate\":\"2025-10-20\",\"endDate\":\"2025-10-24\",\"type\":\"VACATION\",\"reason\":\"Family\"}";

        mvc.perform(post("/api/employees/{id}/absences", empId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/absences/" + absId)))
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.id").value(absId.toString()));
    }

    @Test
    void getOne_returns200_withEtag() throws Exception {
        UUID empId = UUID.randomUUID();
        UUID absId = UUID.randomUUID();
        when(absenceService.getOne(eq(absId), any())).thenReturn(view(absId, empId, 2));

        mvc.perform(get("/api/absences/{id}", absId))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"2\""))
                .andExpect(jsonPath("$.id").value(absId.toString()));
    }

    @Test
    void approve_returns200_withNewEtag() throws Exception {
        UUID empId = UUID.randomUUID();
        UUID absId = UUID.randomUUID();
        var after = view(absId, empId, 1);
        when(absenceService.approve(eq(absId), any(DecisionReq.class), eq("\"0\""), any()))
                .thenReturn(after);

        mvc.perform(put("/api/absences/{id}/approve", absId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"0\"")
                        .content("{\"comment\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void reject_returns200_withNewEtag() throws Exception {
        UUID empId = UUID.randomUUID();
        UUID absId = UUID.randomUUID();
        when(absenceService.reject(eq(absId), any(DecisionReq.class), eq("\"1\""), any()))
                .thenReturn(view(absId, empId, 2));

        mvc.perform(put("/api/absences/{id}/reject", absId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"1\"")
                        .content("{\"comment\":\"nope\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"2\""));
    }

    @Test
    void cancel_returns200_withNewEtag() throws Exception {
        UUID empId = UUID.randomUUID();
        UUID absId = UUID.randomUUID();
        when(absenceService.cancel(eq(absId), any(DecisionReq.class), eq("\"0\""), any()))
                .thenReturn(view(absId, empId, 1));

        mvc.perform(put("/api/absences/{id}/cancel", absId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"0\"")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""));
    }
}
