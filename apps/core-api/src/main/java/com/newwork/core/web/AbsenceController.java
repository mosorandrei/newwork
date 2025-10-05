package com.newwork.core.web;

import com.newwork.core.security.UserPrincipal;
import com.newwork.core.service.AbsenceService;
import com.newwork.core.web.dto.AbsenceDtos.*;
import com.newwork.core.web.support.Etags;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

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

    @GetMapping("/api/employees/{employeeId}/absences")
    public ResponseEntity<List<AbsenceView>> list(@PathVariable UUID employeeId, Authentication auth) {
        var out = service.listForEmployee(employeeId, principal(auth));
        return ResponseEntity.ok(out);
    }

    @PostMapping("/api/employees/{employeeId}/absences")
    public ResponseEntity<AbsenceView> create(@PathVariable UUID employeeId,
                                              @RequestBody CreateAbsenceReq req,
                                              Authentication auth) {
        var out = service.create(employeeId, req, principal(auth));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/absences/{id}").buildAndExpand(out.id()).toUri();
        return ResponseEntity.created(location).eTag(etags.toEtag(out.version())).body(out);
    }

    @GetMapping("/api/absences/{id}")
    public ResponseEntity<AbsenceView> one(@PathVariable UUID id, Authentication auth) {
        var out = service.getOne(id, principal(auth));
        return ResponseEntity.ok().eTag(etags.toEtag(out.version())).body(out);
    }

    @PutMapping("/api/absences/{id}/approve")
    public ResponseEntity<AbsenceView> approve(@PathVariable UUID id,
                                               @RequestBody(required = false) DecisionReq req,
                                               @RequestHeader(value="If-Match", required=false) String ifMatch,
                                               Authentication auth) {
        var out = service.approve(id, req, ifMatch, principal(auth));
        return ResponseEntity.ok().eTag(etags.toEtag(out.version())).body(out);
    }

    @PutMapping("/api/absences/{id}/reject")
    public ResponseEntity<AbsenceView> reject(@PathVariable UUID id,
                                              @RequestBody(required = false) DecisionReq req,
                                              @RequestHeader(value="If-Match", required=false) String ifMatch,
                                              Authentication auth) {
        var out = service.reject(id, req, ifMatch, principal(auth));
        return ResponseEntity.ok().eTag(etags.toEtag(out.version())).body(out);
    }

    @PutMapping("/api/absences/{id}/cancel")
    public ResponseEntity<AbsenceView> cancel(@PathVariable UUID id,
                                              @RequestBody(required = false) DecisionReq req,
                                              @RequestHeader(value="If-Match", required=false) String ifMatch,
                                              Authentication auth) {
        var out = service.cancel(id, req, ifMatch, principal(auth));
        return ResponseEntity.ok().eTag(etags.toEtag(out.version())).body(out);
    }
}
