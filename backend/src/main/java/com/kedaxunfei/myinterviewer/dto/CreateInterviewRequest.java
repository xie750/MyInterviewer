package com.kedaxunfei.myinterviewer.dto;

import jakarta.validation.constraints.NotNull;

public record CreateInterviewRequest(
        @NotNull Long positionId,
        @NotNull Long styleId
) {
}
