package com.kedaxunfei.myinterviewer.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kedaxunfei.myinterviewer.common.ApiResponse;
import com.kedaxunfei.myinterviewer.dto.ResumeParseResponse;
import com.kedaxunfei.myinterviewer.service.ResumeParsingService;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeParsingService resumeParsingService;

    public ResumeController(ResumeParsingService resumeParsingService) {
        this.resumeParsingService = resumeParsingService;
    }

    @PostMapping("/parse")
    public ApiResponse<ResumeParseResponse> parse(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(resumeParsingService.parse(file));
    }
}
