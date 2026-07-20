package com.kedaxunfei.myinterviewer.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kedaxunfei.myinterviewer.common.ApiResponse;
import com.kedaxunfei.myinterviewer.dto.AdminInterviewResponse;
import com.kedaxunfei.myinterviewer.dto.InterviewDetailResponse;
import com.kedaxunfei.myinterviewer.service.InterviewService;

@RestController
@RequestMapping("/api/admin/interviews")
public class AdminInterviewController {

    private final InterviewService interviewService;

    public AdminInterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @GetMapping
    public ApiResponse<List<AdminInterviewResponse>> listAllInterviews() {
        return ApiResponse.success(interviewService.listAllInterviews());
    }

    @GetMapping("/{id}")
    public ApiResponse<InterviewDetailResponse> getInterview(@PathVariable Long id) {
        return ApiResponse.success(interviewService.getAdminInterview(id));
    }
}
