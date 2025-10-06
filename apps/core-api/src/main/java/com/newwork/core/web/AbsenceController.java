package com.newwork.core.web;

import com.newwork.core.security.UserPrincipal;
import com.newwork.core.service.AbsenceService;
import com.newwork.core.web.dto.AbsenceDtos.*;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;
@Tag(name = "Absences")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class AbsenceController {

    private final AbsenceService service;
    private final Etags etags;

    public AbsenceController(AbsenceService service, Etags etags) {
        this.service = service; this.etags = etags;
    }

    private static UserPrincipal principal(Authentication auth) {
        return auth != null && auth.getPrincipal() instanceof UserPrincipal up ? up : null;
    }
    @Operation(summary = "List absences for an employee",
            description = "Allowed: manager, owner.")
    @GetMapping("/api/employees/{employeeId}/absences")
    public ResponseEntity<List<AbsenceView>> list(@PathVariable UUID employeeId, Authentication auth) {
        var out = service.listForEmployee(employeeId, principal(auth));
        return ResponseEntity.ok(out);
    }

    @Operation(summary = "Create absence (owner)",
            description = "Employee creates their own absence request. Returns ETag for future decisions.")
    @ApiResponse(responseCode = "201", description = "Created",
            headers = @Header(name = "ETag", description = "Version of the absence"),
            content = @Content(schema = @Schema(implementation = AbsenceView.class)))
    @PostMapping("/api/employees/{employeeId}/absences")
    public ResponseEntity<AbsenceView> create(@PathVariable UUID employeeId,
                                              @RequestBody CreateAbsenceReq req,
                                              Authentication auth) {
        var out = service.create(employeeId, req, principal(auth));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/absences/{id}").buildAndExpand(out.id()).toUri();
        return ResponseEntity.created(location).eTag(etags.toEtag(out.version())).body(out);
    }

    @Operation(summary = "Get absence by id",
            description = "Returns ETag header.")
    @ApiResponse(responseCode = "200", description = "OK",
            headers = @Header(name = "ETag", description = "Current version"),
            content = @Content(schema = @Schema(implementation = AbsenceView.class)))
    @GetMapping("/api/absences/{id}")
    public ResponseEntity<AbsenceView> one(@PathVariable UUID id, Authentication auth) {
        var out = service.getOne(id, principal(auth));
        return ResponseEntity.ok().eTag(etags.toEtag(out.version())).body(out);
    }
    @Operation(summary = "Approve absence (manager)",
            description = "Requires **If-Match** with current ETag.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approved",
                    headers = @Header(name = "ETag", description = "New version"),
                    content = @Content(schema = @Schema(implementation = com.newwork.core.web.dto.AbsenceDtos.AbsenceView.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "412", description = "Bad If-Match"),
            @ApiResponse(responseCode = "428", description = "If-Match required"),
            @ApiResponse(responseCode = "409", description = "Version mismatch")
    })
    @PutMapping("/api/absences/{id}/approve")
    public ResponseEntity<AbsenceView> approve(@PathVariable UUID id,
                                               @RequestBody(required = false) DecisionReq req,
                                               @RequestHeader(value="If-Match", required=false) String ifMatch,
                                               Authentication auth) {
        var out = service.approve(id, req, ifMatch, principal(auth));
        return ResponseEntity.ok().eTag(etags.toEtag(out.version())).body(out);
    }

    @Operation(summary = "Reject absence (manager)", description = "Requires **If-Match**.")
    @PutMapping("/api/absences/{id}/reject")
    public ResponseEntity<AbsenceView> reject(@PathVariable UUID id,
                                              @RequestBody(required = false) DecisionReq req,
                                              @RequestHeader(value="If-Match", required=false) String ifMatch,
                                              Authentication auth) {
        var out = service.reject(id, req, ifMatch, principal(auth));
        return ResponseEntity.ok().eTag(etags.toEtag(out.version())).body(out);
    }

    @Operation(summary = "Cancel absence (owner)",
            description = "Employee cancels their own pending absence. Requires **If-Match**.")
    @PutMapping("/api/absences/{id}/cancel")
    public ResponseEntity<AbsenceView> cancel(@PathVariable UUID id,
                                              @RequestBody(required = false) DecisionReq req,
                                              @RequestHeader(value="If-Match", required=false) String ifMatch,
                                              Authentication auth) {
        var out = service.cancel(id, req, ifMatch, principal(auth));
        return ResponseEntity.ok().eTag(etags.toEtag(out.version())).body(out);
    }
}
