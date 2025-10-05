package com.newwork.core.web;

import com.newwork.core.security.UserPrincipal;
import com.newwork.core.service.FeedbackService;
import com.newwork.core.web.dto.FeedbackDtos.CreateFeedbackReq;
import com.newwork.core.web.dto.FeedbackDtos.FeedbackView;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees/{id}/feedback")
public class EmployeeFeedbackController {

    private final FeedbackService feedbackService;
    public EmployeeFeedbackController(FeedbackService feedbackService) { this.feedbackService = feedbackService; }

    private static UserPrincipal principal(Authentication auth) {
        return auth != null && auth.getPrincipal() instanceof UserPrincipal up ? up : null;
    }

    // MANAGER or OWNER can view feedback
    @PreAuthorize("hasRole('MANAGER') or T(com.newwork.core.security.Access).isOwner(principal, #id)")
    @GetMapping
    public ResponseEntity<List<FeedbackView>> list(@PathVariable UUID id, Authentication auth) {
        var out = feedbackService.listForEmployee(id, principal(auth));
        return ResponseEntity.ok(out);
    }

    // COWORKER or MANAGER can create feedback
    @PreAuthorize("hasRole('COWORKER') or hasRole('MANAGER')")
    @PostMapping
    public ResponseEntity<FeedbackView> create(@PathVariable UUID id,
                                               @RequestBody CreateFeedbackReq req,
                                               Authentication auth) {
        var out = feedbackService.createForEmployee(id, req, principal(auth));
        return ResponseEntity.status(201).body(out);
    }
}
