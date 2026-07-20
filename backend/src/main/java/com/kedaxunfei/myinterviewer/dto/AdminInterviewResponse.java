package com.kedaxunfei.myinterviewer.dto;

import java.time.LocalDateTime;

import com.kedaxunfei.myinterviewer.domain.InterviewSession;
import com.kedaxunfei.myinterviewer.domain.InterviewStatus;

public record AdminInterviewResponse(
        Long id,
        String username,
        String displayName,
        String positionName,
        String styleName,
        InterviewStatus status,
        Integer questionCount,
        Boolean resumeUsed,
        Integer totalScore,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LocalDateTime updatedAt
) {

    public static AdminInterviewResponse from(
            InterviewSession session,
            String username,
            String displayName,
            String positionName,
            String styleName,
            Integer totalScore
    ) {
        return new AdminInterviewResponse(
                session.getId(),
                username,
                displayName,
                positionName,
                styleName,
                session.getStatus(),
                session.getQuestionCount(),
                Boolean.TRUE.equals(session.getResumeUsed()),
                totalScore,
                session.getStartedAt(),
                session.getEndedAt(),
                session.getUpdatedAt()
        );
    }
}
