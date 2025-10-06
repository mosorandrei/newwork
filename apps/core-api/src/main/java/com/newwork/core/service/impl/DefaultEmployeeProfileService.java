package com.newwork.core.service.impl;

import com.newwork.core.domain.Employee;
import com.newwork.core.domain.EmployeeProfile;
import com.newwork.core.repo.EmployeeProfileRepository;
import com.newwork.core.repo.EmployeeRepository;
import com.newwork.core.security.Access;
import com.newwork.core.security.UserPrincipal;
import com.newwork.core.service.EmployeeProfileService;
import com.newwork.core.web.dto.ProfileDtos.UpdateProfileReq;
import com.newwork.core.web.dto.ProfileDtos.ProfileView;
import com.newwork.core.web.support.Etags;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
@Service
public class DefaultEmployeeProfileService implements EmployeeProfileService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeProfileRepository profileRepository;
    private final Etags etags;

    public DefaultEmployeeProfileService(EmployeeRepository employeeRepository, EmployeeProfileRepository profileRepository, Etags etags) {
        this.employeeRepository = employeeRepository;
        this.profileRepository = profileRepository;
        this.etags = etags;
    }

    @Override
    public ProfileView getProfileByEmployeeId(UUID employeeId, UserPrincipal caller) {
        Employee e = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        EmployeeProfile pr = profileRepository.findByEmployeeId(employeeId).orElse(null);

        if (caller != null && caller.role() == com.newwork.core.security.Role.EMPLOYEE
                && !Access.isOwner(caller, employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        boolean sensitive = Access.canViewSensitive(caller, employeeId);
        return toView(e, pr, sensitive);
    }

    @Override
    @Transactional
    public ProfileView updateProfile(UUID employeeId, UpdateProfileReq req, String ifMatch, UserPrincipal caller) {
        if (!Access.canEditProfile(caller, employeeId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        Employee e = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        EmployeeProfile pr = profileRepository.findByEmployeeId(employeeId).orElseGet(() -> {
            EmployeeProfile x = new EmployeeProfile();
            x.setEmployee(e);
            return x;
        });

        etags.assertMatches(pr.getVersion(), ifMatch);

        if (req.bio() != null) pr.setBio(req.bio());
        if (req.skillsJson() != null) pr.setSkillsJson(req.skillsJson());
        if (req.salary() != null) pr.setSalary(req.salary());
        if (req.ssn() != null) pr.setSsn(req.ssn());
        if (req.address() != null) pr.setAddress(req.address());
        if (req.contactEmail() != null) pr.setContactEmail(req.contactEmail());

        var saved = profileRepository.save(pr);
        return toView(e, saved, true);
    }

    private ProfileView toView(Employee e, EmployeeProfile pr, boolean sensitive) {
        String masked = null;
        if (sensitive && pr != null && pr.getSsn() != null) {
            String s = pr.getSsn();
            masked = s.length() >= 4 ? "****" + s.substring(s.length()-4) : "****";
        }
        return new ProfileView(
                e.getId(),
                pr != null ? pr.getBio() : null,
                pr != null ? pr.getSkillsJson() : null,
                sensitive && pr != null ? pr.getSalary() : null,
                masked,
                sensitive && pr != null ? pr.getAddress() : null,
                pr != null ? pr.getContactEmail() : null,
                pr != null ? pr.getVersion() : 0
        );
    }
}
