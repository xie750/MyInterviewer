package com.kedaxunfei.myinterviewer.dto;

import java.time.LocalDateTime;

import com.kedaxunfei.myinterviewer.domain.InterviewSession;
import com.kedaxunfei.myinterviewer.domain.PostureEvent;
import com.kedaxunfei.myinterviewer.domain.PostureEventType;
import com.kedaxunfei.myinterviewer.domain.PostureSeverity;
import com.kedaxunfei.myinterviewer.domain.SysUser;

public record AdminPostureEventResponse(
        Long id,
        Long sessionId,
        Long userId,
        String username,
        String displayName,
        PostureEventType eventType,
        PostureSeverity severity,
        Integer score,
        String detail,
        LocalDateTime occurredAt,
        LocalDateTime createdAt,
        LocalDateTime sessionStartedAt
) {

    public static AdminPostureEventResponse from(PostureEvent event, SysUser user, InterviewSession session) {
        return new AdminPostureEventResponse(
                event.getId(),
                event.getSessionId(),
                event.getUserId(),
                user == null ? "未知用户" : user.getUsername(),
                user == null ? "未知用户" : user.getDisplayName(),
                event.getEventType(),
                event.getSeverity(),
                event.getScore(),
                event.getDetail(),
                event.getOccurredAt(),
                event.getCreatedAt(),
                session == null ? null : session.getStartedAt()
        );
    }
}
