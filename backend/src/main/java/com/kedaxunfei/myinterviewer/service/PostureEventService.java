package com.kedaxunfei.myinterviewer.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kedaxunfei.myinterviewer.common.BusinessException;
import com.kedaxunfei.myinterviewer.common.ErrorCodes;
import com.kedaxunfei.myinterviewer.domain.InterviewSession;
import com.kedaxunfei.myinterviewer.domain.InterviewStatus;
import com.kedaxunfei.myinterviewer.domain.PostureEvent;
import com.kedaxunfei.myinterviewer.dto.PostureEventRequest;
import com.kedaxunfei.myinterviewer.dto.PostureEventResponse;
import com.kedaxunfei.myinterviewer.repository.InterviewSessionMapper;
import com.kedaxunfei.myinterviewer.repository.PostureEventMapper;
import com.kedaxunfei.myinterviewer.security.AuthenticatedUser;

@Service
public class PostureEventService {

    private final PostureEventMapper postureEventMapper;
    private final InterviewSessionMapper interviewSessionMapper;

    public PostureEventService(PostureEventMapper postureEventMapper, InterviewSessionMapper interviewSessionMapper) {
        this.postureEventMapper = postureEventMapper;
        this.interviewSessionMapper = interviewSessionMapper;
    }

    @Transactional
    public PostureEventResponse reportEvent(AuthenticatedUser user, PostureEventRequest request) {
        InterviewSession session = requireOwnedSession(user, request.interviewId());
        if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "面试已结束，不能继续上报姿态事件");
        }

        LocalDateTime now = LocalDateTime.now();
        PostureEvent event = new PostureEvent();
        event.setSessionId(session.getId());
        event.setUserId(user.id());
        event.setEventType(request.eventType());
        event.setSeverity(request.severity());
        event.setScore(request.score());
        event.setDetail(clean(request.detail()));
        event.setOccurredAt(request.occurredAt() == null ? now : request.occurredAt());
        event.setCreatedAt(now);
        postureEventMapper.insert(event);
        return PostureEventResponse.from(event);
    }

    public List<PostureEventResponse> listOwnEvents(AuthenticatedUser user, Long interviewId) {
        InterviewSession session = requireOwnedSession(user, interviewId);
        return listEventEntities(session.getId()).stream()
                .map(PostureEventResponse::from)
                .toList();
    }

    public List<PostureEventResponse> listEventsForSession(Long sessionId) {
        return listEventEntities(sessionId).stream()
                .map(PostureEventResponse::from)
                .toList();
    }

    private List<PostureEvent> listEventEntities(Long sessionId) {
        return postureEventMapper.selectList(new LambdaQueryWrapper<PostureEvent>()
                .eq(PostureEvent::getSessionId, sessionId)
                .orderByDesc(PostureEvent::getOccurredAt)
                .orderByDesc(PostureEvent::getId));
    }

    private InterviewSession requireOwnedSession(AuthenticatedUser user, Long id) {
        InterviewSession session = interviewSessionMapper.selectById(id);
        if (session == null || !session.getUserId().equals(user.id())) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "面试不存在");
        }
        return session;
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }
}
