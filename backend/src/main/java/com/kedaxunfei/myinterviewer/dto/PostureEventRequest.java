package com.kedaxunfei.myinterviewer.dto;

import java.time.LocalDateTime;

import com.kedaxunfei.myinterviewer.domain.PostureEventType;
import com.kedaxunfei.myinterviewer.domain.PostureSeverity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostureEventRequest(
        @NotNull Long interviewId,
        @NotNull PostureEventType eventType,
        @NotNull PostureSeverity severity,
        @NotNull @Min(0) @Max(100) Integer score,
        @Size(max = 500) String detail,
        LocalDateTime occurredAt
) {
}
