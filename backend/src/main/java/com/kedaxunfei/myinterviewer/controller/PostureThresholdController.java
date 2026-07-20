package com.kedaxunfei.myinterviewer.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kedaxunfei.myinterviewer.common.ApiResponse;
import com.kedaxunfei.myinterviewer.dto.PostureThresholdResponse;
import com.kedaxunfei.myinterviewer.service.PostureThresholdService;

@RestController
@RequestMapping("/api/posture-thresholds")
public class PostureThresholdController {

    private final PostureThresholdService postureThresholdService;

    public PostureThresholdController(PostureThresholdService postureThresholdService) {
        this.postureThresholdService = postureThresholdService;
    }

    @GetMapping
    public ApiResponse<List<PostureThresholdResponse>> listEnabled() {
        return ApiResponse.success(postureThresholdService.listEnabled());
    }
}
