package com.newwork.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newwork.core.domain.User;
import com.newwork.core.security.Role;
import com.newwork.core.security.JwtUtil;
import com.newwork.core.service.UserService;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import({ GlobalExceptionHandler.class })
@TestPropertySource(properties = "app.security.enabled=false")
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean UserService userService;
    @MockBean JwtUtil jwt;

    private static User user(UUID userId, UUID employeeId, Role role, String email) {
        var u = new User();
        u.setId(userId);
        u.setEmployeeId(employeeId);
        u.setRole(role);
        u.setEmail(email);
        u.setPasswordHash("ignored-in-controller");
        return u;
    }

    @Test
    void login_success_returns200_withTokenRoleAndEmployeeId() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID empId  = UUID.randomUUID();
        var email   = "manager@newwork.test";
        var pwd     = "Passw0rd!";
        var u       = user(userId, empId, Role.MANAGER, email);

        when(userService.authenticate(email, pwd)).thenReturn(Optional.of(u));
        when(jwt.sign(userId, Role.MANAGER, empId)).thenReturn("mock.jwt");

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", pwd))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt"))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.employeeId").value(empId.toString()));

        verify(userService).authenticate(email, pwd);
        verify(jwt).sign(userId, Role.MANAGER, empId);
    }

    @Test
    void login_invalidCredentials_returns401_error() throws Exception {
        var email = "wrong@x.test";
        var pwd   = "bad";

        when(userService.authenticate(email, pwd)).thenReturn(Optional.empty());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", pwd))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));

        verify(userService).authenticate(email, pwd);
        verifyNoInteractions(jwt);
    }
}
