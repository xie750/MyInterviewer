package com.kedaxunfei.myinterviewer.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kedaxunfei.myinterviewer.common.ApiResponse;
import com.kedaxunfei.myinterviewer.config.AppProperties;
import com.kedaxunfei.myinterviewer.dto.ResumeParseResponse;
import com.kedaxunfei.myinterviewer.service.ResumeParsingService;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeParsingService resumeParsingService;
    private final AppProperties appProperties;

    public ResumeController(ResumeParsingService resumeParsingService, AppProperties appProperties) {
        this.resumeParsingService = resumeParsingService;
        this.appProperties = appProperties;
    }

    @PostMapping("/parse")
    public ApiResponse<ResumeParseResponse> parse(@RequestParam("file") MultipartFile file) {
        if (!appProperties.feature().resume()) {
            throw new com.kedaxunfei.myinterviewer.common.BusinessException(
                    com.kedaxunfei.myinterviewer.common.ErrorCodes.BAD_REQUEST,
                    "简历解析功能当前未开启，可直接进入面试"
            );
        }
        return ApiResponse.success(resumeParsingService.parse(file));
    }
}
