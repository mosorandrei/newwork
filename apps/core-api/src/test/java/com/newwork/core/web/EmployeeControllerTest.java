package com.newwork.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newwork.core.domain.Employee;
import com.newwork.core.service.EmployeeService;
import com.newwork.core.web.support.Etags;
import com.newwork.core.web.support.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for EmployeeController when a Service layer is used.
 * Security filters are disabled for clarity.
 */
@WebMvcTest(controllers = EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({Etags.class, GlobalExceptionHandler.class})
class EmployeeControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean EmployeeService employeeService;

    // ---------- helpers ----------
    private static Employee emp(UUID id, String first, String last, Integer version) {
        Employee e = new Employee();
        e.setFirstName(first);
        e.setLastName(last);
        e.setId(id);
        e.setVersion(version);
        return e;
    }

    @Test
    void list_returns200_andArray() throws Exception {
        when(employeeService.getAllEmployees())
                .thenReturn(List.of(emp(UUID.randomUUID(), "Alice", "Ng", 0)));

        mvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Alice"))
                .andExpect(jsonPath("$[0].lastName").value("Ng"));
    }

    @Test
    void getById_returns200_andETag() throws Exception {
        UUID id = UUID.randomUUID();
        when(employeeService.findById(id))
                .thenReturn(Optional.of(emp(id, "Bob", "Ionescu", 3)));

        mvc.perform(get("/api/employees/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"3\""))
                .andExpect(jsonPath("$.firstName").value("Bob"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(employeeService.findById(id)).thenReturn(Optional.empty());

        mvc.perform(get("/api/employees/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returns201_withLocation_andETag() throws Exception {
        UUID id = UUID.randomUUID();
        Employee saved = emp(id, "Dana", "Pop", 0);
        when(employeeService.save(any(Employee.class))).thenReturn(saved);

        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
              {"firstName":"Dana","lastName":"Pop"}
            """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/employees/" + id)))
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.firstName").value("Dana"));

        // verify we passed trimmed names to the service
        ArgumentCaptor<Employee> cap = ArgumentCaptor.forClass(Employee.class);
        verify(employeeService).save(cap.capture());
        Employee arg = cap.getValue();
        // fields set, ID is null before persistence, which is fine
        assert arg.getFirstName().equals("Dana");
        assert arg.getLastName().equals("Pop");
    }

    @Test
    void put_missingIfMatch_returns428() throws Exception {
        UUID id = UUID.randomUUID();
        when(employeeService.findById(id)).thenReturn(Optional.of(emp(id, "X", "Y", 1)));

        mvc.perform(put("/api/employees/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Alina\"}"))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.error").value("if_match_required"));
    }

    @Test
    void put_badIfMatch_returns412() throws Exception {
        UUID id = UUID.randomUUID();
        when(employeeService.findById(id)).thenReturn(Optional.of(emp(id, "X", "Y", 1)));

        mvc.perform(put("/api/employees/{id}", id)
                        .header("If-Match", "not-a-number")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastName\":\"Alina\"}"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.error").value("bad_if_match"));
    }

    @Test
    void put_conflict_whenVersionMismatch_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        // current version is 2
        when(employeeService.findById(id)).thenReturn(Optional.of(emp(id, "X", "Y", 2)));

        mvc.perform(put("/api/employees/{id}", id)
                        .header("If-Match", "\"1\"") // stale
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastName\":\"Nguyen\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("version_mismatch"))
                .andExpect(jsonPath("$.currentVersion").value(2));
    }

    @Test
    void put_success_returns200_withNewETag_andBody() throws Exception {
        UUID id = UUID.randomUUID();
        Employee current = emp(id, "Dana", "Pop", 1);
        Employee afterSave = emp(id, "Dana", "Popescu", 2);

        when(employeeService.findById(id)).thenReturn(Optional.of(current));
        when(employeeService.save(any(Employee.class))).thenReturn(afterSave);

        mvc.perform(put("/api/employees/{id}", id)
                        .header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastName\":\"Popescu\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"2\""))
                .andExpect(jsonPath("$.lastName").value("Popescu"));

        verify(employeeService).save(any(Employee.class));
    }

    @Test
    void delete_conflict_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(employeeService.findById(id)).thenReturn(Optional.of(emp(id, "Z", "Q", 5)));

        mvc.perform(delete("/api/employees/{id}", id)
                        .header("If-Match", "\"4\""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("version_mismatch"))
                .andExpect(jsonPath("$.currentVersion").value(5));

        verify(employeeService, never()).delete(any(Employee.class));
    }

    @Test
    void delete_success_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        when(employeeService.findById(id)).thenReturn(Optional.of(emp(id, "Z", "Q", 5)));

        mvc.perform(delete("/api/employees/{id}", id)
                        .header("If-Match", "\"5\""))
                .andExpect(status().isNoContent());

        verify(employeeService).delete(any(Employee.class));
    }
}
