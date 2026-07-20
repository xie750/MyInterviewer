package com.kedaxunfei.myinterviewer.dto;

import com.kedaxunfei.myinterviewer.domain.InterviewerStyle;

public record VirtualHumanResponse(
        String key,
        String name,
        String description,
        String imageUrl,
        String accentColor,
        String badge
) {

    private static final String DEFAULT_KEY = "default-interviewer";
    private static final String DEFAULT_NAME = "AI 面试官";
    private static final String DEFAULT_DESCRIPTION = "基础静态面试官形象，资源不可用时保持占位展示。";

    public static VirtualHumanResponse from(InterviewerStyle style) {
        return new VirtualHumanResponse(
                fallback(style.getVirtualHumanKey(), DEFAULT_KEY),
                fallback(style.getVirtualHumanName(), DEFAULT_NAME),
                fallback(style.getVirtualHumanDescription(), DEFAULT_DESCRIPTION),
                null,
                null,
                null
        );
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
