package com.kedaxunfei.myinterviewer.dto;

import com.kedaxunfei.myinterviewer.domain.SysUser;
import com.kedaxunfei.myinterviewer.domain.UserRole;

public record UserResponse(
        Long id,
        String username,
        String displayName,
        UserRole role
) {

    public static UserResponse from(SysUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }
}

