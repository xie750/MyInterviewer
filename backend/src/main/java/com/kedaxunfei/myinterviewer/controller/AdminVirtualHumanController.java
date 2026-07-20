package com.kedaxunfei.myinterviewer.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kedaxunfei.myinterviewer.common.ApiResponse;
import com.kedaxunfei.myinterviewer.dto.VirtualHumanAssetRequest;
import com.kedaxunfei.myinterviewer.dto.VirtualHumanAssetResponse;
import com.kedaxunfei.myinterviewer.service.VirtualHumanAssetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/virtual-humans")
public class AdminVirtualHumanController {

    private final VirtualHumanAssetService virtualHumanAssetService;

    public AdminVirtualHumanController(VirtualHumanAssetService virtualHumanAssetService) {
        this.virtualHumanAssetService = virtualHumanAssetService;
    }

    @GetMapping
    public ApiResponse<List<VirtualHumanAssetResponse>> listAssets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(virtualHumanAssetService.listAssets(keyword, enabled));
    }

    @PostMapping
    public ApiResponse<VirtualHumanAssetResponse> createAsset(@Valid @RequestBody VirtualHumanAssetRequest request) {
        return ApiResponse.success(virtualHumanAssetService.createAsset(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<VirtualHumanAssetResponse> updateAsset(
            @PathVariable Long id,
            @Valid @RequestBody VirtualHumanAssetRequest request) {
        return ApiResponse.success(virtualHumanAssetService.updateAsset(id, request));
    }
}
