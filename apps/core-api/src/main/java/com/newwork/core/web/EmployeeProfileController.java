package com.newwork.core.web;

import com.newwork.core.security.UserPrincipal;
import com.newwork.core.service.EmployeeProfileService;
import com.newwork.core.web.dto.ProfileDtos.ProfileView;
import com.newwork.core.web.dto.ProfileDtos.UpdateProfileReq;
import com.newwork.core.web.support.Etags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
@Tag(name = "Profiles")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/employees/{id}/profile")
public class EmployeeProfileController {

    private final EmployeeProfileService employeeProfileService;
    private final Etags etags;
    public EmployeeProfileController(EmployeeProfileService employeeProfileService, Etags etags){ this.employeeProfileService = employeeProfileService; this.etags = etags; }

    private static UserPrincipal principal(Authentication auth) {
        return auth != null && auth.getPrincipal() instanceof UserPrincipal up ? up : null;
    }

    @Operation(summary = "Get profile",
            description = """
        Visibility depends on role:
        - Manager or Owner → unmasked sensitive fields.
        - Coworker → masked sensitive fields.
        - Employee (not owner) → 403.
      """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    headers = @Header(name = "ETag", description = "Profile version"),
                    content = @Content(schema = @Schema(implementation = ProfileView.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @PreAuthorize("hasRole('MANAGER') or hasRole('COWORKER') or T(com.newwork.core.security.Access).isOwner(principal, #id)")
    @GetMapping
    public ResponseEntity<ProfileView> get(@PathVariable UUID id, Authentication auth) {
        var view = employeeProfileService.getProfileByEmployeeId(id, principal(auth));
        return ResponseEntity.ok().eTag(etags.toEtag(view.version())).body(view);
    }
    @Operation(summary = "Update profile (manager or owner)",
            description = "Requires **If-Match** header from GET.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    headers = @Header(name = "ETag", description = "New version"),
                    content = @Content(schema = @Schema(implementation = ProfileView.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "412", description = "Bad If-Match"),
            @ApiResponse(responseCode = "428", description = "If-Match required"),
            @ApiResponse(responseCode = "409", description = "Version mismatch")
    })
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
