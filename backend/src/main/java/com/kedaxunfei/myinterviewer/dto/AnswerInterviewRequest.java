package com.kedaxunfei.myinterviewer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerInterviewRequest(
        @NotBlank
        @Size(max = 4000)
        String content
) {
}
