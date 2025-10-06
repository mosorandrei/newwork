package com.newwork.core.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newwork.core.CoreApiApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;

import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CoreApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("it")
class AuthAndEmployeesIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String managerToken;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> "jdbc:h2:mem:it_employees;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
        r.add("spring.datasource.username", () -> "sa");
        r.add("spring.datasource.password", () -> "");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        r.add("spring.jpa.properties.hibernate.default_schema", () -> "PUBLIC");
        r.add("app.auth.hmacSecret", () -> "it-secret");
        r.add("app.ai.hf.token", () -> "dummy");
    }

    @BeforeEach
    void loginManager() throws Exception {
        var res = mvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"manager@newwork.test\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode j = om.readTree(res.getResponse().getContentAsByteArray());
        managerToken = j.get("token").asText();
        assertNotNull(managerToken);
    }

    private String bearer() { return "Bearer " + managerToken; }

    @Test
    void employees_crud_with_etag() throws Exception {
        // CREATE
        var create = mvc.perform(post("/api/employees")
                        .header("Authorization", bearer())
                        .contentType(APPLICATION_JSON)
                        .content("{\"firstName\":\"Dana\",\"lastName\":\"Pop\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("ETag", "\"0\""))
                .andReturn();

        var location = create.getResponse().getHeader("Location");
        assert location != null;
        var id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        // GET
        var get1 = mvc.perform(get(location).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andReturn();
        var etag0 = get1.getResponse().getHeader("ETag");

        // UPDATE (If-Match)
        var upd = mvc.perform(put(location)
                        .header("Authorization", bearer())
                        .header("If-Match", etag0)
                        .contentType(APPLICATION_JSON)
                        .content("{\"lastName\":\"Popescu\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.lastName").value("Popescu"))
                .andReturn();
        var etag1 = upd.getResponse().getHeader("ETag");

        // DELETE (If-Match)
        mvc.perform(delete(location)
                        .header("Authorization", bearer())
                        .header("If-Match", etag1))
                .andExpect(status().isNoContent());

        // verify gone
        mvc.perform(get(location).header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_employees_ok() throws Exception {
        mvc.perform(get("/api/employees").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }
}
