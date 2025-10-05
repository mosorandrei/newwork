package com.newwork.core.service;

import com.newwork.core.security.UserPrincipal;
import com.newwork.core.web.dto.FeedbackDtos.CreateFeedbackReq;
import com.newwork.core.web.dto.FeedbackDtos.FeedbackView;

import java.util.List;
import java.util.UUID;

public interface FeedbackService {
    List<FeedbackView> listForEmployee(UUID employeeId, UserPrincipal caller);
    FeedbackView createForEmployee(UUID employeeId, CreateFeedbackReq req, UserPrincipal caller);
}
