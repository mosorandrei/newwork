package com.newwork.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newwork.core.security.JwtAuthFilter;
import com.newwork.core.security.JwtUtil;
import com.newwork.core.security.UserPrincipal;
import com.newwork.core.service.EmployeeProfileService;
import com.newwork.core.web.dto.ProfileDtos.ProfileView;
import com.newwork.core.web.dto.ProfileDtos.UpdateProfileReq;
import com.newwork.core.web.support.Etags;
import com.newwork.core.web.support.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = EmployeeProfileController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import({Etags.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "app.security.enabled=false")
class EmployeeProfileControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockBean
    JwtUtil jwtUtil;

    @MockBean EmployeeProfileService profileService;

    private static ProfileView view(UUID empId, int version) {
        return new ProfileView(
                empId,
                "Great teammate",                         // bio
                "{\"skills\":[\"Java\",\"Spring\"]}",     // skillsJson
                new BigDecimal("12345.67"),               // salary (may be null for coworker)
                "****6789",                               // ssnMasked (or null for coworker)
                "Some Street 1",                          // address (or null for coworker)
                "alice@newwork.test",                     // contactEmail
                version
        );
    }

    @Test
    void get_returns200_etag_and_body() throws Exception {
        UUID id = UUID.randomUUID();
        when(profileService.getProfileByEmployeeId(eq(id), any()))
                .thenReturn(view(id, 3));

        mvc.perform(get("/api/employees/{id}/profile", id))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"3\""))
                .andExpect(jsonPath("$.employeeId", is(id.toString())))
                .andExpect(jsonPath("$.bio", is("Great teammate")))
                .andExpect(jsonPath("$.contactEmail", is("alice@newwork.test")));

        // verify principal(null) was passed through
        verify(profileService).getProfileByEmployeeId(eq(id), isNull());
    }

    @Test
    void get_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(profileService.getProfileByEmployeeId(eq(id), any()))
                .thenThrow(new ResponseStatusException(NOT_FOUND));

        mvc.perform(get("/api/employees/{id}/profile", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_success_returns200_newEtag_and_body() throws Exception {
        UUID id = UUID.randomUUID();
        var reqBody = new UpdateProfileReq("Updated bio", null, null, null, null, "new@mail.test");

        when(profileService.updateProfile(eq(id), any(UpdateProfileReq.class), eq("\"1\""), any()))
                .thenReturn(new ProfileView(
                        id,
                        "Updated bio",
                        "{\"skills\":[\"Java\"]}",
                        new BigDecimal("12345.67"),
                        "****6789",
                        "Some Street 1",
                        "new@mail.test",
                        2
                ));

        mvc.perform(put("/api/employees/{id}/profile", id)
                        .header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "bio", "Updated bio",
                                "contactEmail", "new@mail.test"
                        ))))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"2\""))
                .andExpect(jsonPath("$.bio").value("Updated bio"))
                .andExpect(jsonPath("$.contactEmail").value("new@mail.test"));

        // capture and verify params forwarded correctly
        ArgumentCaptor<UpdateProfileReq> reqCap = ArgumentCaptor.forClass(UpdateProfileReq.class);
        ArgumentCaptor<String> ifmCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UserPrincipal> principalCap = ArgumentCaptor.forClass(UserPrincipal.class);

        verify(profileService).updateProfile(eq(id), reqCap.capture(), ifmCap.capture(), principalCap.capture());
        var captured = reqCap.getValue();
        assert "Updated bio".equals(captured.bio());
        assert "new@mail.test".equals(captured.contactEmail());
        assert "\"1\"".equals(ifmCap.getValue());
        assert principalCap.getValue() == null;
    }

    @Test
    void put_missingIfMatch_propagates428() throws Exception {
        UUID id = UUID.randomUUID();
        when(profileService.updateProfile(eq(id), any(), isNull(), any()))
                .thenThrow(new ResponseStatusException(PRECONDITION_REQUIRED, "if_match_required"));

        mvc.perform(put("/api/employees/{id}/profile", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("bio", "x"))))
                .andExpect(status().isPreconditionRequired());
    }

    @Test
    void put_conflict_versionMismatch_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(profileService.updateProfile(eq(id), any(), eq("\"1\""), any()))
                .thenThrow(new ResponseStatusException(CONFLICT, "version_mismatch"));

        mvc.perform(put("/api/employees/{id}/profile", id)
                        .header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("bio", "x"))))
                .andExpect(status().isConflict());
    }
}
