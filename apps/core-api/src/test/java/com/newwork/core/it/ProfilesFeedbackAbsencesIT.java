package com.newwork.core.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newwork.core.CoreApiApplication;
import com.newwork.core.service.AiPolishService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CoreApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc()
@ActiveProfiles("it")
@Import(ProfilesFeedbackAbsencesIT.AiMock.class)
class ProfilesFeedbackAbsencesIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String mgrTok;
    private String bobTok;
    private String carolTok;

    private UUID bobEmpId;
    private UUID aliceEmpId;

    @TestConfiguration
    static class AiMock {
        @Bean @Primary
        AiPolishService aiPolishService() {
            return new AiPolishService() {
                @Override public String polish(String text) { return "[MOCK] " + text.trim(); }
                @Override public String modelId() { return "mock-ai"; }
            };
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () ->
                "jdbc:h2:mem:it_profiles;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;" +
                        "INIT=CREATE SCHEMA IF NOT EXISTS PUBLIC\\;SET SCHEMA PUBLIC");
        r.add("spring.datasource.username", () -> "sa");
        r.add("spring.datasource.password", () -> "");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        r.add("spring.jpa.properties.hibernate.default_schema", () -> "PUBLIC");
        r.add("app.auth.hmacSecret", () -> "it-secret");
        r.add("app.ai.hf.token", () -> "dummy");
    }

    @BeforeEach
    void loginAll() throws Exception {
        var m = login("manager@newwork.test", "Passw0rd!");
        mgrTok = m.token; aliceEmpId = m.employeeId;

        var b = login("bob@newwork.test", "Passw0rd!");
        bobTok = b.token; bobEmpId = b.employeeId;

        var c = login("carol@newwork.test", "Passw0rd!");
        carolTok = c.token;

        assertNotNull(mgrTok); assertNotNull(bobTok); assertNotNull(carolTok);
    }

    private record LoginOut(String token, UUID employeeId) {}
    private LoginOut login(String email, String pwd) throws Exception {
        var res = mvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + pwd + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode j = om.readTree(res.getResponse().getContentAsByteArray());
        return new LoginOut(j.get("token").asText(), UUID.fromString(j.get("employeeId").asText()));
    }
    private static String bearer(String tok){ return "Bearer " + tok; }

    @Test
    void profile_visibility_rules() throws Exception {
        mvc.perform(get("/api/employees/{id}/profile", aliceEmpId)
                        .header("Authorization", bearer(bobTok)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/employees/{id}/profile", aliceEmpId)
                        .header("Authorization", bearer(carolTok)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salary").doesNotExist())
                .andExpect(jsonPath("$.ssnMasked").doesNotExist());

        mvc.perform(get("/api/employees/{id}/profile", bobEmpId)
                        .header("Authorization", bearer(bobTok)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salary").exists());
    }

    @Test
    void profile_update_with_if_match_by_manager() throws Exception {

        var r1 = mvc.perform(get("/api/employees/{id}/profile", aliceEmpId)
                        .header("Authorization", bearer(mgrTok)))
                .andExpect(status().isOk())
                .andReturn();
        var etag0 = r1.getResponse().getHeader("ETag");


        mvc.perform(put("/api/employees/{id}/profile", aliceEmpId)
                        .header("Authorization", bearer(mgrTok))
                        .header("If-Match", etag0)
                        .contentType(APPLICATION_JSON)
                        .content("{\"bio\":\"Updated by manager\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Updated by manager"));

        mvc.perform(get("/api/employees/{id}/profile", aliceEmpId)
                        .header("Authorization", bearer(mgrTok)))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", not(etag0)));
    }

    @Test
    void feedback_create_and_list() throws Exception {
        mvc.perform(post("/api/employees/{id}/feedback", bobEmpId)
                        .header("Authorization", bearer(carolTok))
                        .contentType(APPLICATION_JSON)
                        .content("{\"text\":\"great job on the release\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.textPolished").value(startsWith("[MOCK]")));

        mvc.perform(get("/api/employees/{id}/feedback", bobEmpId)
                        .header("Authorization", bearer(bobTok)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].textPolished", startsWith("[MOCK]")));

        mvc.perform(get("/api/employees/{id}/feedback", bobEmpId)
                        .header("Authorization", bearer(mgrTok)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void absence_create_by_owner_and_approve_by_manager() throws Exception {
        var cr = mvc.perform(post("/api/employees/{id}/absences", bobEmpId)
                        .header("Authorization", bearer(bobTok))
                        .contentType(APPLICATION_JSON)
                        .content("{\"startDate\":\"2025-10-20\",\"endDate\":\"2025-10-24\",\"type\":\"VACATION\",\"reason\":\"Trip\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andReturn();
        var etag0 = cr.getResponse().getHeader("ETag");
        var node = om.readTree(cr.getResponse().getContentAsByteArray());
        var absId = UUID.fromString(node.get("id").asText());

        mvc.perform(put("/api/absences/{id}/approve", absId)
                        .header("Authorization", bearer(mgrTok))
                        .header("If-Match", etag0)
                        .contentType(APPLICATION_JSON)
                        .content("{\"note\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
