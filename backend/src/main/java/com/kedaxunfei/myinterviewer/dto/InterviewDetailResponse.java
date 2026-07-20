package com.kedaxunfei.myinterviewer.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.kedaxunfei.myinterviewer.domain.InterviewSession;
import com.kedaxunfei.myinterviewer.domain.InterviewStatus;

public record InterviewDetailResponse(
        Long id,
        InterviewStatus status,
        Integer questionCount,
        PositionResponse position,
        InterviewerStyleResponse style,
        List<InterviewMessageResponse> messages,
        InterviewReportResponse report,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LocalDateTime updatedAt
) {

    public static InterviewDetailResponse from(
            InterviewSession session,
            PositionResponse position,
            InterviewerStyleResponse style,
            List<InterviewMessageResponse> messages,
            InterviewReportResponse report
    ) {
        return new InterviewDetailResponse(
                session.getId(),
                session.getStatus(),
                session.getQuestionCount(),
                position,
                style,
                messages,
                report,
                session.getStartedAt(),
                session.getEndedAt(),
                session.getUpdatedAt()
        );
    }
}
