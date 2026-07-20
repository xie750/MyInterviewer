package com.kedaxunfei.myinterviewer.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kedaxunfei.myinterviewer.common.ApiResponse;
import com.kedaxunfei.myinterviewer.dto.PostureThresholdRequest;
import com.kedaxunfei.myinterviewer.dto.PostureThresholdResponse;
import com.kedaxunfei.myinterviewer.service.PostureThresholdService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/posture-thresholds")
public class AdminPostureThresholdController {

    private final PostureThresholdService postureThresholdService;

    public AdminPostureThresholdController(PostureThresholdService postureThresholdService) {
        this.postureThresholdService = postureThresholdService;
    }

    @GetMapping
    public ApiResponse<List<PostureThresholdResponse>> listAll() {
        return ApiResponse.success(postureThresholdService.listAll());
    }

    @PutMapping("/{id}")
    public ApiResponse<PostureThresholdResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PostureThresholdRequest request) {
        return ApiResponse.success(postureThresholdService.update(id, request));
    }
}
