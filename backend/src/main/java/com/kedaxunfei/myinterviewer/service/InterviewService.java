package com.kedaxunfei.myinterviewer.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kedaxunfei.myinterviewer.common.BusinessException;
import com.kedaxunfei.myinterviewer.common.ErrorCodes;
import com.kedaxunfei.myinterviewer.domain.InterviewMessage;
import com.kedaxunfei.myinterviewer.domain.InterviewReport;
import com.kedaxunfei.myinterviewer.domain.InterviewSession;
import com.kedaxunfei.myinterviewer.domain.InterviewStatus;
import com.kedaxunfei.myinterviewer.domain.InterviewerStyle;
import com.kedaxunfei.myinterviewer.domain.JobPosition;
import com.kedaxunfei.myinterviewer.domain.MessageRole;
import com.kedaxunfei.myinterviewer.domain.SysUser;
import com.kedaxunfei.myinterviewer.dto.AdminInterviewResponse;
import com.kedaxunfei.myinterviewer.dto.AnswerInterviewRequest;
import com.kedaxunfei.myinterviewer.dto.CreateInterviewRequest;
import com.kedaxunfei.myinterviewer.dto.InterviewDetailResponse;
import com.kedaxunfei.myinterviewer.dto.InterviewMessageResponse;
import com.kedaxunfei.myinterviewer.dto.InterviewReportResponse;
import com.kedaxunfei.myinterviewer.dto.InterviewSummaryResponse;
import com.kedaxunfei.myinterviewer.dto.InterviewerStyleResponse;
import com.kedaxunfei.myinterviewer.dto.PositionResponse;
import com.kedaxunfei.myinterviewer.dto.ResumeContextRequest;
import com.kedaxunfei.myinterviewer.dto.ResumeContextResponse;
import com.kedaxunfei.myinterviewer.integration.InterviewAiReport;
import com.kedaxunfei.myinterviewer.integration.InterviewAiService;
import com.kedaxunfei.myinterviewer.integration.ResumeContext;
import com.kedaxunfei.myinterviewer.repository.InterviewMessageMapper;
import com.kedaxunfei.myinterviewer.repository.InterviewReportMapper;
import com.kedaxunfei.myinterviewer.repository.InterviewSessionMapper;
import com.kedaxunfei.myinterviewer.repository.SysUserMapper;
import com.kedaxunfei.myinterviewer.security.AuthenticatedUser;

@Service
public class InterviewService {

    private final InterviewSessionMapper interviewSessionMapper;
    private final InterviewMessageMapper interviewMessageMapper;
    private final InterviewReportMapper interviewReportMapper;
    private final SysUserMapper sysUserMapper;
    private final PositionService positionService;
    private final InterviewerStyleService interviewerStyleService;
    private final InterviewAiService interviewAiService;

    public InterviewService(
            InterviewSessionMapper interviewSessionMapper,
            InterviewMessageMapper interviewMessageMapper,
            InterviewReportMapper interviewReportMapper,
            SysUserMapper sysUserMapper,
            PositionService positionService,
            InterviewerStyleService interviewerStyleService,
            InterviewAiService interviewAiService
    ) {
        this.interviewSessionMapper = interviewSessionMapper;
        this.interviewMessageMapper = interviewMessageMapper;
        this.interviewReportMapper = interviewReportMapper;
        this.sysUserMapper = sysUserMapper;
        this.positionService = positionService;
        this.interviewerStyleService = interviewerStyleService;
        this.interviewAiService = interviewAiService;
    }

    @Transactional
    public InterviewDetailResponse createInterview(AuthenticatedUser user, CreateInterviewRequest request) {
        JobPosition position = positionService.requireEnabledPosition(request.positionId());
        InterviewerStyle style = interviewerStyleService.requireEnabledStyle(request.styleId());
        ResumeContext resume = toResumeContext(request.resume());
        LocalDateTime now = LocalDateTime.now();

        InterviewSession session = new InterviewSession();
        session.setUserId(user.id());
        session.setPositionId(position.getId());
        session.setStyleId(style.getId());
        applyResumeContext(session, resume);
        session.setStatus(InterviewStatus.IN_PROGRESS);
        session.setQuestionCount(1);
        session.setStartedAt(now);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        interviewSessionMapper.insert(session);

        saveMessage(session.getId(), MessageRole.ASSISTANT, interviewAiService.generateOpeningQuestion(position, style, resume), 1);
        return buildDetail(session.getId());
    }

    public List<InterviewSummaryResponse> listOwnInterviews(AuthenticatedUser user) {
        return interviewSessionMapper.selectList(new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getUserId, user.id())
                        .orderByDesc(InterviewSession::getUpdatedAt)
                        .orderByDesc(InterviewSession::getId))
                .stream()
                .map(this::buildSummary)
                .toList();
    }

    public InterviewDetailResponse getOwnInterview(AuthenticatedUser user, Long id) {
        InterviewSession session = requireOwnedSession(user, id);
        return buildDetail(session.getId());
    }

    @Transactional
    public InterviewDetailResponse answerInterview(AuthenticatedUser user, Long id, AnswerInterviewRequest request) {
        InterviewSession session = requireOwnedSession(user, id);
        if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "面试已结束，不能继续回答");
        }

        JobPosition position = positionService.requirePosition(session.getPositionId());
        InterviewerStyle style = interviewerStyleService.requireStyle(session.getStyleId());
        ResumeContext resume = resumeContextFromSession(session);
        int currentRound = session.getQuestionCount();
        String answer = request.content().trim();
        saveMessage(session.getId(), MessageRole.USER, answer, currentRound);

        List<InterviewMessage> history = listMessages(session.getId());
        int nextQuestionNo = currentRound + 1;
        String followUp = interviewAiService.generateFollowUpQuestion(position, style, resume, history, answer, nextQuestionNo);
        saveMessage(session.getId(), MessageRole.ASSISTANT, followUp, nextQuestionNo);

        session.setQuestionCount(nextQuestionNo);
        session.setUpdatedAt(LocalDateTime.now());
        interviewSessionMapper.updateById(session);
        return buildDetail(session.getId());
    }

    @Transactional
    public InterviewDetailResponse finishInterview(AuthenticatedUser user, Long id) {
        InterviewSession session = requireOwnedSession(user, id);
        if (session.getStatus() == InterviewStatus.IN_PROGRESS) {
            createReportIfAbsent(session);
            LocalDateTime now = LocalDateTime.now();
            session.setStatus(InterviewStatus.COMPLETED);
            session.setEndedAt(now);
            clearTemporaryResumeContext(session);
            session.setUpdatedAt(now);
            updateFinishedSessionAndClearResume(session);
        }
        return buildDetail(session.getId());
    }

    public InterviewReportResponse getOwnReport(AuthenticatedUser user, Long id) {
        InterviewSession session = requireOwnedSession(user, id);
        InterviewReport report = requireReport(session.getId());
        return InterviewReportResponse.from(report);
    }

    public List<AdminInterviewResponse> listAllInterviews() {
        return interviewSessionMapper.selectList(new LambdaQueryWrapper<InterviewSession>()
                        .orderByDesc(InterviewSession::getUpdatedAt)
                        .orderByDesc(InterviewSession::getId))
                .stream()
                .map(this::buildAdminSummary)
                .toList();
    }

    public InterviewDetailResponse getAdminInterview(Long id) {
        InterviewSession session = requireSession(id);
        return buildDetail(session.getId());
    }

    private void createReportIfAbsent(InterviewSession session) {
        if (findReport(session.getId()) != null) {
            return;
        }
        JobPosition position = positionService.requirePosition(session.getPositionId());
        InterviewerStyle style = interviewerStyleService.requireStyle(session.getStyleId());
        ResumeContext resume = resumeContextFromSession(session);
        InterviewAiReport aiReport = interviewAiService.generateReport(position, style, resume, listMessages(session.getId()));
        LocalDateTime now = LocalDateTime.now();

        InterviewReport report = new InterviewReport();
        report.setSessionId(session.getId());
        report.setUserId(session.getUserId());
        report.setTotalScore(aiReport.totalScore());
        report.setTechnicalScore(aiReport.technicalScore());
        report.setCommunicationScore(aiReport.communicationScore());
        report.setLogicScore(aiReport.logicScore());
        report.setSummary(aiReport.summary());
        report.setStrengths(aiReport.strengths());
        report.setImprovements(aiReport.improvements());
        report.setRecommendation(aiReport.recommendation());
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        interviewReportMapper.insert(report);
    }

    private InterviewSummaryResponse buildSummary(InterviewSession session) {
        JobPosition position = positionService.requirePosition(session.getPositionId());
        InterviewerStyle style = interviewerStyleService.requireStyle(session.getStyleId());
        InterviewReport report = findReport(session.getId());
        return InterviewSummaryResponse.from(
                session,
                position.getName(),
                style.getName(),
                session.getResumeUsed(),
                report == null ? null : report.getTotalScore()
        );
    }

    private AdminInterviewResponse buildAdminSummary(InterviewSession session) {
        SysUser user = sysUserMapper.selectById(session.getUserId());
        JobPosition position = positionService.requirePosition(session.getPositionId());
        InterviewerStyle style = interviewerStyleService.requireStyle(session.getStyleId());
        InterviewReport report = findReport(session.getId());
        return AdminInterviewResponse.from(
                session,
                user == null ? "未知用户" : user.getUsername(),
                user == null ? "未知用户" : user.getDisplayName(),
                position.getName(),
                style.getName(),
                report == null ? null : report.getTotalScore()
        );
    }

    private InterviewDetailResponse buildDetail(Long sessionId) {
        InterviewSession session = requireSession(sessionId);
        JobPosition position = positionService.requirePosition(session.getPositionId());
        InterviewerStyle style = interviewerStyleService.requireStyle(session.getStyleId());
        List<InterviewMessageResponse> messages = listMessages(session.getId()).stream()
                .map(InterviewMessageResponse::from)
                .toList();
        return InterviewDetailResponse.from(
                session,
                PositionResponse.from(position),
                InterviewerStyleResponse.from(style),
                ResumeContextResponse.from(session),
                messages,
                InterviewReportResponse.from(findReport(session.getId()))
        );
    }

    private void saveMessage(Long sessionId, MessageRole role, String content, Integer roundNo) {
        InterviewMessage message = new InterviewMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setRoundNo(roundNo);
        message.setCreatedAt(LocalDateTime.now());
        interviewMessageMapper.insert(message);
    }

    private List<InterviewMessage> listMessages(Long sessionId) {
        return interviewMessageMapper.selectList(new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByAsc(InterviewMessage::getRoundNo)
                .orderByAsc(InterviewMessage::getId));
    }

    private void applyResumeContext(InterviewSession session, ResumeContext resume) {
        if (resume == null || !resume.present()) {
            session.setResumeUsed(false);
            return;
        }
        session.setResumeUsed(true);
        session.setResumeSummary(limit(resume.summary(), 1000));
        session.setResumeSkills(joinLimited(resume.skills(), 1000));
        session.setResumeProjects(joinLimited(resume.projects(), 1000));
        session.setResumeWarnings(joinLimited(resume.warnings(), 1000));
    }

    private ResumeContext toResumeContext(ResumeContextRequest request) {
        if (request == null) {
            return ResumeContext.empty();
        }
        return new ResumeContext(
                clean(request.summary()),
                cleanList(request.skills(), 12),
                cleanList(request.projects(), 5),
                cleanList(request.warnings(), 4)
        );
    }

    private ResumeContext resumeContextFromSession(InterviewSession session) {
        if (!Boolean.TRUE.equals(session.getResumeUsed())) {
            return ResumeContext.empty();
        }
        return new ResumeContext(
                clean(session.getResumeSummary()),
                splitLines(session.getResumeSkills()),
                splitLines(session.getResumeProjects()),
                splitLines(session.getResumeWarnings())
        );
    }

    private void clearTemporaryResumeContext(InterviewSession session) {
        session.setResumeSummary(null);
        session.setResumeSkills(null);
        session.setResumeProjects(null);
        session.setResumeWarnings(null);
    }

    private void updateFinishedSessionAndClearResume(InterviewSession session) {
        interviewSessionMapper.update(null, new LambdaUpdateWrapper<InterviewSession>()
                .eq(InterviewSession::getId, session.getId())
                .set(InterviewSession::getStatus, session.getStatus())
                .set(InterviewSession::getEndedAt, session.getEndedAt())
                .set(InterviewSession::getUpdatedAt, session.getUpdatedAt())
                .set(InterviewSession::getResumeSummary, null)
                .set(InterviewSession::getResumeSkills, null)
                .set(InterviewSession::getResumeProjects, null)
                .set(InterviewSession::getResumeWarnings, null));
    }

    private List<String> cleanList(List<String> values, int maxSize) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(this::clean)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .limit(maxSize)
                .toList();
    }

    private List<String> splitLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("\\n")).stream()
                .map(this::clean)
                .filter(item -> item != null && !item.isBlank())
                .toList();
    }

    private String joinLimited(List<String> values, int maxLength) {
        return limit(String.join("\n", values), maxLength);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private InterviewSession requireOwnedSession(AuthenticatedUser user, Long id) {
        InterviewSession session = requireSession(id);
        if (!session.getUserId().equals(user.id())) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "面试不存在");
        }
        return session;
    }

    private InterviewSession requireSession(Long id) {
        InterviewSession session = interviewSessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "面试不存在");
        }
        return session;
    }

    private InterviewReport requireReport(Long sessionId) {
        InterviewReport report = findReport(sessionId);
        if (report == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "报告尚未生成");
        }
        return report;
    }

    private InterviewReport findReport(Long sessionId) {
        return interviewReportMapper.selectOne(new LambdaQueryWrapper<InterviewReport>()
                .eq(InterviewReport::getSessionId, sessionId)
                .last("limit 1"));
    }
}
