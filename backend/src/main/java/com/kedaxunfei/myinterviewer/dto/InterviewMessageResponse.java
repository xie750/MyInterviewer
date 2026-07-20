package com.kedaxunfei.myinterviewer.dto;

import java.time.LocalDateTime;

import com.kedaxunfei.myinterviewer.domain.InterviewMessage;
import com.kedaxunfei.myinterviewer.domain.MessageRole;

public record InterviewMessageResponse(
        Long id,
        MessageRole role,
        String content,
        Integer roundNo,
        LocalDateTime createdAt
) {

    public static InterviewMessageResponse from(InterviewMessage message) {
        return new InterviewMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getRoundNo(),
                message.getCreatedAt()
        );
    }
}
