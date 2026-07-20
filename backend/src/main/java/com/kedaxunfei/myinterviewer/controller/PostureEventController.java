package com.kedaxunfei.myinterviewer.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kedaxunfei.myinterviewer.common.ApiResponse;
import com.kedaxunfei.myinterviewer.dto.PostureEventRequest;
import com.kedaxunfei.myinterviewer.dto.PostureEventResponse;
import com.kedaxunfei.myinterviewer.security.AuthenticatedUser;
import com.kedaxunfei.myinterviewer.service.PostureEventService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class PostureEventController {

    private final PostureEventService postureEventService;

    public PostureEventController(PostureEventService postureEventService) {
        this.postureEventService = postureEventService;
    }

    @PostMapping("/posture-events")
    public ApiResponse<PostureEventResponse> reportEvent(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PostureEventRequest request
    ) {
        return ApiResponse.success(postureEventService.reportEvent(user, request));
    }

    @GetMapping("/interviews/{id}/posture-events")
    public ApiResponse<List<PostureEventResponse>> listOwnEvents(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id
    ) {
        return ApiResponse.success(postureEventService.listOwnEvents(user, id));
    }
}
