package com.kedaxunfei.myinterviewer.dto;

import java.time.LocalDateTime;

import com.kedaxunfei.myinterviewer.domain.InterviewReport;

public record InterviewReportResponse(
        Long id,
        Long sessionId,
        Integer totalScore,
        Integer technicalScore,
        Integer communicationScore,
        Integer logicScore,
        String summary,
        String strengths,
        String improvements,
        String recommendation,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static InterviewReportResponse from(InterviewReport report) {
        if (report == null) {
            return null;
        }
        return new InterviewReportResponse(
                report.getId(),
                report.getSessionId(),
                report.getTotalScore(),
                report.getTechnicalScore(),
                report.getCommunicationScore(),
                report.getLogicScore(),
                report.getSummary(),
                report.getStrengths(),
                report.getImprovements(),
                report.getRecommendation(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
