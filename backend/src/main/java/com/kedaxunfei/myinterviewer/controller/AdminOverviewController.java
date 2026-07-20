package com.kedaxunfei.myinterviewer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kedaxunfei.myinterviewer.common.ApiResponse;
import com.kedaxunfei.myinterviewer.dto.AdminOverviewResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminOverviewController {

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewResponse> overview() {
        return ApiResponse.success(new AdminOverviewResponse("ready", "管理员权限入口已受保护"));
    }
}

