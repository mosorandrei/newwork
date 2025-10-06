package com.newwork.core.web.dto;

import java.time.Instant;
import java.util.UUID;

public final class FeedbackDtos {
    private FeedbackDtos(){}

    public record CreateFeedbackReq(String text) {}
    public record FeedbackView(UUID id, UUID authorEmployeeId,
                               String textOriginal, String textPolished,
                               String polishModel, Instant createdAt) {}
}
