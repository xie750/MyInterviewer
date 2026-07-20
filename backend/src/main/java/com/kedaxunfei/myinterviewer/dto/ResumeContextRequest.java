package com.kedaxunfei.myinterviewer.dto;

import java.util.List;

import jakarta.validation.constraints.Size;

public record ResumeContextRequest(
        @Size(max = 500) String summary,
        @Size(max = 20) List<@Size(max = 50) String> skills,
        @Size(max = 8) List<@Size(max = 120) String> projects,
        @Size(max = 8) List<@Size(max = 120) String> warnings
) {
}
