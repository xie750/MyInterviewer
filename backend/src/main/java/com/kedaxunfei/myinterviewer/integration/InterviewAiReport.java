package com.kedaxunfei.myinterviewer.integration;

public record InterviewAiReport(
        Integer totalScore,
        Integer technicalScore,
        Integer communicationScore,
        Integer logicScore,
        String summary,
        String strengths,
        String improvements,
        String recommendation
) {
}
