package com.newwork.core.service;

import com.newwork.core.security.UserPrincipal;
import com.newwork.core.web.dto.ProfileDtos.UpdateProfileReq;
import com.newwork.core.web.dto.ProfileDtos.ProfileView;

import java.util.UUID;

public interface EmployeeProfileService {
    ProfileView getProfileByEmployeeId(UUID employeeId, UserPrincipal caller);
    ProfileView updateProfile(UUID employeeId, UpdateProfileReq req, String ifMatch, UserPrincipal caller);
}
