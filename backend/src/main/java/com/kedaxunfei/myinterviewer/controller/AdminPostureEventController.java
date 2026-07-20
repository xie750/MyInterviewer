package com.kedaxunfei.myinterviewer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kedaxunfei.myinterviewer.common.ApiResponse;
import com.kedaxunfei.myinterviewer.domain.PostureEventType;
import com.kedaxunfei.myinterviewer.domain.PostureSeverity;
import com.kedaxunfei.myinterviewer.dto.AdminPostureEventResponse;
import com.kedaxunfei.myinterviewer.dto.PageResponse;
import com.kedaxunfei.myinterviewer.service.PostureEventService;

@RestController
@RequestMapping("/api/admin/posture-events")
public class AdminPostureEventController {

    private final PostureEventService postureEventService;

    public AdminPostureEventController(PostureEventService postureEventService) {
        this.postureEventService = postureEventService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminPostureEventResponse>> listEvents(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PostureEventType eventType,
            @RequestParam(required = false) PostureSeverity severity,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(postureEventService.listAdminEvents(
                sessionId,
                keyword,
                eventType,
                severity,
                page,
                pageSize
        ));
    }
}
