package com.kedaxunfei.myinterviewer.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long total,
        int page,
        int pageSize
) {

    public static <T> PageResponse<T> of(List<T> allItems, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(Math.min(pageSize, 100), 1);
        int fromIndex = Math.min((safePage - 1) * safePageSize, allItems.size());
        int toIndex = Math.min(fromIndex + safePageSize, allItems.size());
        return new PageResponse<>(
                allItems.subList(fromIndex, toIndex),
                allItems.size(),
                safePage,
                safePageSize
        );
    }
}
