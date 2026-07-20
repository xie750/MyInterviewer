package com.kedaxunfei.myinterviewer.dto;

import java.util.List;

import com.kedaxunfei.myinterviewer.domain.InterviewSession;

public record ResumeContextResponse(
        boolean used,
        String summary,
        List<String> skills,
        List<String> projects,
        List<String> warnings
) {

    public static ResumeContextResponse empty() {
        return new ResumeContextResponse(false, null, List.of(), List.of(), List.of());
    }

    public static ResumeContextResponse from(InterviewSession session) {
        if (!Boolean.TRUE.equals(session.getResumeUsed())) {
            return empty();
        }
        return new ResumeContextResponse(
                true,
                session.getResumeSummary(),
                split(session.getResumeSkills()),
                split(session.getResumeProjects()),
                split(session.getResumeWarnings())
        );
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("\\n")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
