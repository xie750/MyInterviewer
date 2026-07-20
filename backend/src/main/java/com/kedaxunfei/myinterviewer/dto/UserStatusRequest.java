package com.kedaxunfei.myinterviewer.dto;

import com.kedaxunfei.myinterviewer.domain.UserStatus;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull UserStatus status
) {
}
