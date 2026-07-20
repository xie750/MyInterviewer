package com.kedaxunfei.myinterviewer.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kedaxunfei.myinterviewer.common.ApiResponse;
import com.kedaxunfei.myinterviewer.dto.InterviewerStyleResponse;
import com.kedaxunfei.myinterviewer.service.InterviewerStyleService;

@RestController
@RequestMapping("/api/interviewer-styles")
public class InterviewerStyleController {

    private final InterviewerStyleService interviewerStyleService;

    public InterviewerStyleController(InterviewerStyleService interviewerStyleService) {
        this.interviewerStyleService = interviewerStyleService;
    }

    @GetMapping
    public ApiResponse<List<InterviewerStyleResponse>> listStyles() {
        return ApiResponse.success(interviewerStyleService.listEnabledStyles());
    }
}
