package com.kedaxunfei.myinterviewer.dto;

import java.time.LocalDateTime;

import com.kedaxunfei.myinterviewer.domain.InterviewerStyle;

public record InterviewerStyleResponse(
        Long id,
        String name,
        String description,
        String promptTemplate,
        String scenario,
        VirtualHumanResponse virtualHuman,
        Boolean enabled,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static InterviewerStyleResponse from(InterviewerStyle style) {
        return new InterviewerStyleResponse(
                style.getId(),
                style.getName(),
                style.getDescription(),
                style.getPromptTemplate(),
                style.getScenario(),
                VirtualHumanResponse.from(style),
                style.getEnabled(),
                style.getSortOrder(),
                style.getCreatedAt(),
                style.getUpdatedAt()
        );
    }
}
