package com.newwork.core.service.impl;

import com.newwork.core.domain.Employee;
import com.newwork.core.domain.Feedback;
import com.newwork.core.repo.EmployeeRepository;
import com.newwork.core.repo.FeedbackRepository;
import com.newwork.core.security.Access;
import com.newwork.core.security.Role;
import com.newwork.core.security.UserPrincipal;
import com.newwork.core.service.AiPolishService;
import com.newwork.core.service.FeedbackService;
import com.newwork.core.web.dto.FeedbackDtos.CreateFeedbackReq;
import com.newwork.core.web.dto.FeedbackDtos.FeedbackView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class DefaultFeedbackService implements FeedbackService {

    private final EmployeeRepository employeeRepository;
    private final FeedbackRepository feedbackRepository;
    private final AiPolishService ai;

    public DefaultFeedbackService(EmployeeRepository employeeRepository,
                                  FeedbackRepository feedbackRepository,
                                  AiPolishService ai) {
        this.employeeRepository = employeeRepository;
        this.feedbackRepository = feedbackRepository;
        this.ai = ai;
    }

    @Override
    public List<FeedbackView> listForEmployee(UUID employeeId, UserPrincipal caller) {
        if (caller == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        boolean owner = Access.isOwner(caller, employeeId);
        if (!(caller.role() == Role.MANAGER || owner)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var list = feedbackRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
        return list.stream().map(this::toView).toList();
    }

    @Override
    public FeedbackView createForEmployee(UUID employeeId, CreateFeedbackReq req, UserPrincipal caller) {
        if (caller == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (!(caller.role() == Role.COWORKER || caller.role() == Role.MANAGER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Employee target = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String original = req.text() == null ? "" : req.text().trim();
        if (original.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text_required");

        String polished = ai.polish(original);

        var f = new Feedback();
        f.setEmployee(target);
        f.setAuthorEmployeeId(caller.employeeId());
        f.setTextOriginal(original);
        f.setTextPolished(polished);
        f.setPolishModel(ai.modelId());

        var saved = feedbackRepository.save(f);
        return toView(saved);
    }

    private FeedbackView toView(Feedback f) {
        return new FeedbackView(
                f.getId(),
                f.getAuthorEmployeeId(),
                f.getTextOriginal(),
                f.getTextPolished(),
                f.getPolishModel(),
                f.getCreatedAt()
        );
    }
}
