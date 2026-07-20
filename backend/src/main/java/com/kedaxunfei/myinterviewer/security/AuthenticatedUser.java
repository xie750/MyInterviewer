package com.kedaxunfei.myinterviewer.security;

import com.kedaxunfei.myinterviewer.domain.UserRole;

public record AuthenticatedUser(Long id, String username, UserRole role) {
}

