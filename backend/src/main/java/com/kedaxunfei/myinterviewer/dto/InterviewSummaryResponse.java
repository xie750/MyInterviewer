package com.kedaxunfei.myinterviewer.dto;

import java.time.LocalDateTime;

import com.kedaxunfei.myinterviewer.domain.InterviewSession;
import com.kedaxunfei.myinterviewer.domain.InterviewStatus;

public record InterviewSummaryResponse(
        Long id,
        InterviewStatus status,
        Integer questionCount,
        String positionName,
        String styleName,
        Integer totalScore,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LocalDateTime updatedAt
) {

    public static InterviewSummaryResponse from(
            InterviewSession session,
            String positionName,
            String styleName,
            Integer totalScore
    ) {
        return new InterviewSummaryResponse(
                session.getId(),
                session.getStatus(),
                session.getQuestionCount(),
                positionName,
                styleName,
                totalScore,
                session.getStartedAt(),
                session.getEndedAt(),
                session.getUpdatedAt()
        );
    }
}
