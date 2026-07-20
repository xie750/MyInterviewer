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
import com.kedaxunfei.myinterviewer.domain.PostureEventType;
import com.kedaxunfei.myinterviewer.domain.PostureSeverity;
import com.kedaxunfei.myinterviewer.domain.SysUser;
import com.kedaxunfei.myinterviewer.dto.AdminPostureEventResponse;
import com.kedaxunfei.myinterviewer.dto.PageResponse;
import com.kedaxunfei.myinterviewer.dto.PostureEventRequest;
import com.kedaxunfei.myinterviewer.dto.PostureEventResponse;
import com.kedaxunfei.myinterviewer.repository.InterviewSessionMapper;
import com.kedaxunfei.myinterviewer.repository.PostureEventMapper;
import com.kedaxunfei.myinterviewer.repository.SysUserMapper;
import com.kedaxunfei.myinterviewer.security.AuthenticatedUser;

@Service
public class PostureEventService {

    private final PostureEventMapper postureEventMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final SysUserMapper sysUserMapper;

    public PostureEventService(
            PostureEventMapper postureEventMapper,
            InterviewSessionMapper interviewSessionMapper,
            SysUserMapper sysUserMapper) {
        this.postureEventMapper = postureEventMapper;
        this.interviewSessionMapper = interviewSessionMapper;
        this.sysUserMapper = sysUserMapper;
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

    public PageResponse<AdminPostureEventResponse> listAdminEvents(
            Long sessionId,
            String keyword,
            PostureEventType eventType,
            PostureSeverity severity,
            int page,
            int pageSize
    ) {
        LambdaQueryWrapper<PostureEvent> wrapper = new LambdaQueryWrapper<PostureEvent>()
                .eq(sessionId != null, PostureEvent::getSessionId, sessionId)
                .eq(eventType != null, PostureEvent::getEventType, eventType)
                .eq(severity != null, PostureEvent::getSeverity, severity)
                .orderByDesc(PostureEvent::getOccurredAt)
                .orderByDesc(PostureEvent::getId);

        List<AdminPostureEventResponse> responses = postureEventMapper.selectList(wrapper).stream()
                .map(event -> {
                    SysUser user = sysUserMapper.selectById(event.getUserId());
                    InterviewSession session = interviewSessionMapper.selectById(event.getSessionId());
                    return AdminPostureEventResponse.from(event, user, session);
                })
                .filter(event -> matchesKeyword(event, keyword))
                .toList();
        return PageResponse.of(responses, page, pageSize);
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

    private boolean matchesKeyword(AdminPostureEventResponse event, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        return contains(event.username(), normalized)
                || contains(event.displayName(), normalized)
                || contains(event.detail(), normalized)
                || event.sessionId().toString().contains(normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }
}
