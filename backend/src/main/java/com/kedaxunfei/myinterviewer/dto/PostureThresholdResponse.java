package com.kedaxunfei.myinterviewer.dto;

import java.time.LocalDateTime;

import com.kedaxunfei.myinterviewer.domain.PostureEventType;
import com.kedaxunfei.myinterviewer.domain.PostureThresholdConfig;

public record PostureThresholdResponse(
        Long id,
        PostureEventType eventType,
        String displayName,
        String description,
        Integer warningThreshold,
        Integer criticalThreshold,
        Boolean enabled,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PostureThresholdResponse from(PostureThresholdConfig config) {
        return new PostureThresholdResponse(
                config.getId(),
                config.getEventType(),
                config.getDisplayName(),
                config.getDescription(),
                config.getWarningThreshold(),
                config.getCriticalThreshold(),
                config.getEnabled(),
                config.getSortOrder(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
