package com.kedaxunfei.myinterviewer.dto;

import java.time.LocalDateTime;

import com.kedaxunfei.myinterviewer.domain.VirtualHumanAsset;

public record VirtualHumanAssetResponse(
        Long id,
        String assetKey,
        String name,
        String description,
        String imageUrl,
        String accentColor,
        String badge,
        Boolean enabled,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static VirtualHumanAssetResponse from(VirtualHumanAsset asset) {
        return new VirtualHumanAssetResponse(
                asset.getId(),
                asset.getAssetKey(),
                asset.getName(),
                asset.getDescription(),
                asset.getImageUrl(),
                asset.getAccentColor(),
                asset.getBadge(),
                asset.getEnabled(),
                asset.getSortOrder(),
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }
}
