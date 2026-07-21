package com.kedaxunfei.myinterviewer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

public record CreateInterviewRequest(
        @NotNull Long positionId,
        @NotNull Long styleId,
        String resumeFileName,
        @Valid ResumeContextRequest resume
) {
}
