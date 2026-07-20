package com.kedaxunfei.myinterviewer.dto;

import java.time.LocalDateTime;

import com.kedaxunfei.myinterviewer.domain.PostureEvent;
import com.kedaxunfei.myinterviewer.domain.PostureEventType;
import com.kedaxunfei.myinterviewer.domain.PostureSeverity;

public record PostureEventResponse(
        Long id,
        Long sessionId,
        PostureEventType eventType,
        PostureSeverity severity,
        Integer score,
        String detail,
        LocalDateTime occurredAt,
        LocalDateTime createdAt
) {

    public static PostureEventResponse from(PostureEvent event) {
        if (event == null) {
            return null;
        }
        return new PostureEventResponse(
                event.getId(),
                event.getSessionId(),
                event.getEventType(),
                event.getSeverity(),
                event.getScore(),
                event.getDetail(),
                event.getOccurredAt(),
                event.getCreatedAt()
        );
    }
}
