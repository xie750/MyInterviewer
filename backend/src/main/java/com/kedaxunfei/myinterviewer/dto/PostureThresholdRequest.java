package com.kedaxunfei.myinterviewer.dto;

import com.kedaxunfei.myinterviewer.domain.PostureEventType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostureThresholdRequest(
        @NotNull PostureEventType eventType,
        @NotBlank @Size(max = 80) String displayName,
        @Size(max = 500) String description,
        @Min(0) @Max(1000) Integer warningThreshold,
        @Min(0) @Max(1000) Integer criticalThreshold,
        Boolean enabled,
        Integer sortOrder
) {
}
