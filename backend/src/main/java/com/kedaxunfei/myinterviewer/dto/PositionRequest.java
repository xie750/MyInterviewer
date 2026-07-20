package com.kedaxunfei.myinterviewer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PositionRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 500) String description,
        @Size(max = 200) String techStack,
        @Size(max = 40) String difficulty,
        @Size(max = 1000) String promptTemplate,
        Boolean enabled,
        Integer sortOrder) {
}
