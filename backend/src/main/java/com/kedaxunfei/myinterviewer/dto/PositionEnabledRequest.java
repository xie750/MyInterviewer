package com.kedaxunfei.myinterviewer.dto;

import jakarta.validation.constraints.NotNull;

public record PositionEnabledRequest(@NotNull Boolean enabled) {
}
