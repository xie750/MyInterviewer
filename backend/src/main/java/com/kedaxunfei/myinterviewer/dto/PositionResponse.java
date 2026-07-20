package com.kedaxunfei.myinterviewer.dto;

import java.time.LocalDateTime;

import com.kedaxunfei.myinterviewer.domain.JobPosition;

public record PositionResponse(
        Long id,
        String name,
        String description,
        String techStack,
        String difficulty,
        String promptTemplate,
        Boolean enabled,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static PositionResponse from(JobPosition position) {
        return new PositionResponse(
                position.getId(),
                position.getName(),
                position.getDescription(),
                position.getTechStack(),
                position.getDifficulty(),
                position.getPromptTemplate(),
                position.getEnabled(),
                position.getSortOrder(),
                position.getCreatedAt(),
                position.getUpdatedAt());
    }
}
