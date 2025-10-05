package com.newwork.core.service;

import com.newwork.core.domain.Employee;
import com.newwork.core.domain.EmployeeProfile;
import com.newwork.core.repo.EmployeeProfileRepository;
import com.newwork.core.repo.EmployeeRepository;
import com.newwork.core.security.Access;
import com.newwork.core.service.impl.DefaultEmployeeProfileService;
import com.newwork.core.web.dto.ProfileDtos.ProfileView;
import com.newwork.core.web.dto.ProfileDtos.UpdateProfileReq;
import com.newwork.core.web.support.Etags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultEmployeeProfileServiceTest {

    private EmployeeRepository employeeRepo;
    private EmployeeProfileRepository profileRepo;
    private Etags etags;
    private DefaultEmployeeProfileService service;
    private MockedStatic<Access> accessMock;

    @BeforeEach
    void setUp() {
        employeeRepo = mock(EmployeeRepository.class);
        profileRepo  = mock(EmployeeProfileRepository.class);
        etags        = mock(Etags.class);
        service      = new DefaultEmployeeProfileService(employeeRepo, profileRepo, etags);
        accessMock = Mockito.mockStatic(Access.class, Mockito.CALLS_REAL_METHODS);
    }

    @AfterEach
    void tearDown() {
        if (accessMock != null) accessMock.close();
    }

    private static Employee emp(UUID id, String fn, String ln) {
        var e = new Employee();
        e.setId(id);
        e.setFirstName(fn);
        e.setLastName(ln);
        e.setVersion(0);
        return e;
    }

    private static EmployeeProfile prof(Employee e, int version,
                                        String bio, String skillsJson,
                                        BigDecimal salary, String ssn,
                                        String address, String contactEmail) {
        var p = new EmployeeProfile();
        p.setEmployee(e);
        p.setBio(bio);
        p.setSkillsJson(skillsJson);
        p.setSalary(salary);
        p.setSsn(ssn);
        p.setAddress(address);
        p.setContactEmail(contactEmail);
        p.setVersion(version);
        return p;
    }

    @Test
    void getProfile_coworker_or_anonymous_receives_masked_fields() {
        UUID empId = UUID.randomUUID();
        var e  = emp(empId, "Alice", "Ng");
        var pr = prof(e, 5, "Bio", "{\"skills\":[\"Java\"]}",
                new BigDecimal("12345.67"), "123456789", "Str. 1", "alice@newwork.test");

        when(employeeRepo.findById(empId)).thenReturn(Optional.of(e));
        when(profileRepo.findByEmployeeId(empId)).thenReturn(Optional.of(pr));
        accessMock.when(() -> Access.canViewSensitive(any(), eq(empId))).thenReturn(false);

        ProfileView view = service.getProfileByEmployeeId(empId, null);

        assertThat(view.employeeId()).isEqualTo(empId);
        assertThat(view.version()).isEqualTo(5);
        assertThat(view.bio()).isEqualTo("Bio");
        assertThat(view.skillsJson()).isEqualTo("{\"skills\":[\"Java\"]}");
        assertThat(view.salary()).isNull();
        assertThat(view.ssnMasked()).isNull();
        assertThat(view.address()).isNull();
        assertThat(view.contactEmail()).isEqualTo("alice@newwork.test");
    }

    @Test
    void getProfile_manager_or_owner_receives_sensitive_fields_and_maskedSsn() {
        UUID empId = UUID.randomUUID();
        var e  = emp(empId, "Bob", "Ionescu");
        var pr = prof(e, 7, "Great teammate", "{\"skills\":[\"Spring\"]}",
                new BigDecimal("77777.77"), "987654321", "Bd. Unirii 10", "bob@newwork.test");

        when(employeeRepo.findById(empId)).thenReturn(Optional.of(e));
        when(profileRepo.findByEmployeeId(empId)).thenReturn(Optional.of(pr));
        accessMock.when(() -> Access.canViewSensitive(any(), eq(empId))).thenReturn(true);

        ProfileView view = service.getProfileByEmployeeId(empId, null);

        assertThat(view.salary()).isEqualByComparingTo("77777.77");
        assertThat(view.address()).isEqualTo("Bd. Unirii 10");
        assertThat(view.ssnMasked()).isEqualTo("****4321");
    }

    @Test
    void getProfile_employeeNotFound_throws404() {
        UUID empId = UUID.randomUUID();
        when(employeeRepo.findById(empId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfileByEmployeeId(empId, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void updateProfile_forbidden_when_cannotEdit() {
        UUID empId = UUID.randomUUID();
        var req = new UpdateProfileReq("x", null, null, null, null, null);

        accessMock.when(() -> Access.canEditProfile(any(), eq(empId))).thenReturn(false);

        assertThatThrownBy(() -> service.updateProfile(empId, req, "\"1\"", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
        verifyNoInteractions(employeeRepo, profileRepo, etags);
    }

    @Test
    void updateProfile_employeeNotFound_throws404() {
        UUID empId = UUID.randomUUID();
        var req = new UpdateProfileReq("x", null, null, null, null, null);

        accessMock.when(() -> Access.canEditProfile(any(), eq(empId))).thenReturn(true);
        when(employeeRepo.findById(empId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfile(empId, req, "\"1\"", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void updateProfile_ifMatch_mismatch_throws_preconditionFailed() {
        UUID empId = UUID.randomUUID();
        var e  = emp(empId, "Dana", "Pop");
        var pr = prof(e, 2, "bio", null, null, null, null, null);
        var req = new UpdateProfileReq("updated", null, null, null, null, null);

        accessMock.when(() -> Access.canEditProfile(any(), eq(empId))).thenReturn(true);
        when(employeeRepo.findById(empId)).thenReturn(Optional.of(e));
        when(profileRepo.findByEmployeeId(empId)).thenReturn(Optional.of(pr));
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.PRECONDITION_FAILED, "bad_if_match"))
                .when(etags).assertMatches(eq(2), eq("\"1\""));

        assertThatThrownBy(() -> service.updateProfile(empId, req, "\"1\"", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("412");

        verify(profileRepo, never()).save(any());
    }

    @Test
    void updateProfile_success_updates_fields_and_returns_sensitive_view() {
        UUID empId = UUID.randomUUID();
        var e  = emp(empId, "Dana", "Pop");
        var existing = prof(e, 1, "old bio", "{\"skills\":[\"A\"]}",
                new BigDecimal("100.00"), "123456789", "Old St", "dana@nw.test");

        var req = new UpdateProfileReq(
                "new bio",
                "{\"skills\":[\"A\",\"B\"]}",
                new BigDecimal("150.00"),
                "987654321",
                "New Ave 2",
                "new@nw.test"
        );

        var saved = prof(e, 2, "new bio", "{\"skills\":[\"A\",\"B\"]}",
                new BigDecimal("150.00"), "987654321", "New Ave 2", "new@nw.test");

        accessMock.when(() -> Access.canEditProfile(any(), eq(empId))).thenReturn(true);
        when(employeeRepo.findById(empId)).thenReturn(Optional.of(e));
        when(profileRepo.findByEmployeeId(empId)).thenReturn(Optional.of(existing));
        doNothing().when(etags).assertMatches(eq(1), eq("\"1\""));
        when(profileRepo.save(any(EmployeeProfile.class))).thenReturn(saved);

        ProfileView view = service.updateProfile(empId, req, "\"1\"", null);

        assertThat(view.employeeId()).isEqualTo(empId);
        assertThat(view.version()).isEqualTo(2);
        assertThat(view.bio()).isEqualTo("new bio");
        assertThat(view.skillsJson()).isEqualTo("{\"skills\":[\"A\",\"B\"]}");
        assertThat(view.salary()).isEqualByComparingTo("150.00");
        assertThat(view.address()).isEqualTo("New Ave 2");
        assertThat(view.contactEmail()).isEqualTo("new@nw.test");
        assertThat(view.ssnMasked()).isEqualTo("****4321");

        verify(profileRepo).save(argThat(p ->
                "new bio".equals(p.getBio()) &&
                        "{\"skills\":[\"A\",\"B\"]}".equals(p.getSkillsJson()) &&
                        new BigDecimal("150.00").compareTo(p.getSalary()) == 0 &&
                        "987654321".equals(p.getSsn()) &&
                        "New Ave 2".equals(p.getAddress()) &&
                        "new@nw.test".equals(p.getContactEmail())
        ));
    }

    @Test
    void updateProfile_creates_profile_if_missing_and_applies_etag_rule() {
        UUID empId = UUID.randomUUID();
        var e  = emp(empId, "Eli", "Q");
        var req = new UpdateProfileReq("bio", null, null, null, null, "eli@nw.test");

        accessMock.when(() -> Access.canEditProfile(any(), eq(empId))).thenReturn(true);
        when(employeeRepo.findById(empId)).thenReturn(Optional.of(e));
        when(profileRepo.findByEmployeeId(empId)).thenReturn(Optional.empty());
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.PRECONDITION_REQUIRED, "if_match_required"))
                .when(etags).assertMatches(isNull(), isNull());

        assertThatThrownBy(() -> service.updateProfile(empId, req, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("428");

        verify(profileRepo, never()).save(any());
    }
}
