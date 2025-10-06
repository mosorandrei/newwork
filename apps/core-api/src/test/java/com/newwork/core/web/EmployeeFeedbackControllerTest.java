package com.newwork.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newwork.core.security.JwtAuthFilter;
import com.newwork.core.security.JwtUtil;
import com.newwork.core.service.FeedbackService;
import com.newwork.core.web.dto.FeedbackDtos.FeedbackView;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = EmployeeFeedbackController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class EmployeeFeedbackControllerTest {

    @Autowired ObjectMapper om;
    @Autowired
    MockMvc mvc;

    @MockBean FeedbackService feedbackService;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtUtil jwtUtil;

    @Test
    void list_returns200() throws Exception {
        UUID emp = UUID.randomUUID();
        var v = new FeedbackView(UUID.randomUUID(), UUID.randomUUID(),
                "ok", "Okay.", "vennify/t5-base-grammar-correction", Instant.now());
        Mockito.when(feedbackService.listForEmployee(any(), any()))
                .thenReturn(List.of(v));

        mvc.perform(get("/api/employees/{id}/feedback", emp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].textPolished").value("Okay."));
    }

    @Test
    void create_returns201() throws Exception {
        UUID emp = UUID.randomUUID();
        var v = new FeedbackView(UUID.randomUUID(), UUID.randomUUID(),
                "needs improvemnt", "Needs improvement.", "vennify/t5-base-grammar-correction", Instant.now());
        Mockito.when(feedbackService.createForEmployee(any(), any(), any()))
                .thenReturn(v);

        mvc.perform(post("/api/employees/{id}/feedback", emp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"needs improvemnt\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.textPolished").value("Needs improvement."))
                .andExpect(jsonPath("$.polishModel").value("vennify/t5-base-grammar-correction"));
    }
}
