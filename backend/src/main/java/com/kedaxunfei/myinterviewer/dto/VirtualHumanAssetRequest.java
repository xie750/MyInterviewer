package com.kedaxunfei.myinterviewer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VirtualHumanAssetRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,79}$") String assetKey,
        @NotBlank @Size(max = 80) String name,
        @Size(max = 500) String description,
        @Size(max = 500) String imageUrl,
        @Size(max = 20) String accentColor,
        @Size(max = 40) String badge,
        Boolean enabled,
        Integer sortOrder
) {
}
