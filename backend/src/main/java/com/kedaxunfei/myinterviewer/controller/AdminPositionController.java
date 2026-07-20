package com.kedaxunfei.myinterviewer.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kedaxunfei.myinterviewer.common.ApiResponse;
import com.kedaxunfei.myinterviewer.dto.PositionEnabledRequest;
import com.kedaxunfei.myinterviewer.dto.PositionRequest;
import com.kedaxunfei.myinterviewer.dto.PositionResponse;
import com.kedaxunfei.myinterviewer.service.PositionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/positions")
public class AdminPositionController {

    private final PositionService positionService;

    public AdminPositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    public ApiResponse<List<PositionResponse>> listAllPositions() {
        return ApiResponse.success(positionService.listAllPositions());
    }

    @PostMapping
    public ApiResponse<PositionResponse> createPosition(@Valid @RequestBody PositionRequest request) {
        return ApiResponse.success(positionService.createPosition(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PositionResponse> updatePosition(
            @PathVariable Long id,
            @Valid @RequestBody PositionRequest request) {
        return ApiResponse.success(positionService.updatePosition(id, request));
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<PositionResponse> updateEnabled(
            @PathVariable Long id,
            @Valid @RequestBody PositionEnabledRequest request) {
        return ApiResponse.success(positionService.updateEnabled(id, request));
    }
}
