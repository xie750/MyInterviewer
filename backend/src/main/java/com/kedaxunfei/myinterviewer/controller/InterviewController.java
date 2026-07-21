package com.kedaxunfei.myinterviewer.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kedaxunfei.myinterviewer.common.ApiResponse;
import com.kedaxunfei.myinterviewer.dto.AnswerInterviewRequest;
import com.kedaxunfei.myinterviewer.dto.CreateInterviewRequest;
import com.kedaxunfei.myinterviewer.dto.InterviewDetailResponse;
import com.kedaxunfei.myinterviewer.dto.InterviewSummaryResponse;
import com.kedaxunfei.myinterviewer.dto.PageResponse;
import com.kedaxunfei.myinterviewer.security.AuthenticatedUser;
import com.kedaxunfei.myinterviewer.service.InterviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @GetMapping
    public ApiResponse<PageResponse<InterviewSummaryResponse>> listOwnInterviews(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int pageSize
    ) {
        return ApiResponse.success(interviewService.listOwnInterviews(user, page, pageSize));
    }

    @PostMapping
    public ApiResponse<InterviewDetailResponse> createInterview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateInterviewRequest request
    ) {
        return ApiResponse.success(interviewService.createInterview(user, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<InterviewDetailResponse> getInterview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id
    ) {
        return ApiResponse.success(interviewService.getOwnInterview(user, id));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<InterviewDetailResponse> answerInterview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @Valid @RequestBody AnswerInterviewRequest request
    ) {
        return ApiResponse.success(interviewService.answerInterview(user, id, request));
    }

    @PostMapping("/{id}/finish")
    public ApiResponse<InterviewDetailResponse> finishInterview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id
    ) {
        return ApiResponse.success(interviewService.finishInterview(user, id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteInterview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id
    ) {
        interviewService.deleteInterview(user, id);
        return ApiResponse.success();
    }
}
