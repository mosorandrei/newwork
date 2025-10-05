package com.newwork.core.web;

import com.newwork.core.security.UserPrincipal;
import com.newwork.core.service.EmployeeProfileService;
import com.newwork.core.web.dto.ProfileDtos.ProfileView;
import com.newwork.core.web.dto.ProfileDtos.UpdateProfileReq;
import com.newwork.core.web.support.Etags;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/employees/{id}/profile")
public class EmployeeProfileController {

    private final EmployeeProfileService employeeProfileService;
    private final Etags etags;
    public EmployeeProfileController(EmployeeProfileService employeeProfileService, Etags etags){ this.employeeProfileService = employeeProfileService; this.etags = etags; }

    private static UserPrincipal principal(Authentication auth) {
        return auth != null && auth.getPrincipal() instanceof UserPrincipal up ? up : null;
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('COWORKER') or T(com.newwork.core.security.Access).isOwner(principal, #id)")
    @GetMapping
    public ResponseEntity<ProfileView> get(@PathVariable UUID id, Authentication auth) {
        var view = employeeProfileService.getProfileByEmployeeId(id, principal(auth));
        return ResponseEntity.ok().eTag(etags.toEtag(view.version())).body(view);
    }

    @PreAuthorize("hasRole('MANAGER') or T(com.newwork.core.security.Access).isOwner(principal, #id)")
    @PutMapping
    public ResponseEntity<ProfileView> update(@PathVariable UUID id,
                                              @RequestBody UpdateProfileReq body,
                                              @RequestHeader(value="If-Match", required=false) String ifMatch,
                                              Authentication auth) {
        var view = employeeProfileService.updateProfile(id, body, ifMatch, principal(auth));
        return ResponseEntity.ok().eTag(etags.toEtag(view.version())).body(view);
    }
}
