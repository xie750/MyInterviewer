package com.kedaxunfei.myinterviewer.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kedaxunfei.myinterviewer.common.BusinessException;
import com.kedaxunfei.myinterviewer.common.ErrorCodes;
import com.kedaxunfei.myinterviewer.domain.SysUser;
import com.kedaxunfei.myinterviewer.domain.UserStatus;
import com.kedaxunfei.myinterviewer.dto.LoginRequest;
import com.kedaxunfei.myinterviewer.dto.LoginResponse;
import com.kedaxunfei.myinterviewer.dto.UserResponse;
import com.kedaxunfei.myinterviewer.repository.SysUserMapper;
import com.kedaxunfei.myinterviewer.security.AuthenticatedUser;
import com.kedaxunfei.myinterviewer.security.JwtTokenProvider;

@Service
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        SysUser user = findByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() != UserStatus.ENABLED) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "账号已被禁用");
        }

        String token = jwtTokenProvider.createToken(user);
        return new LoginResponse(token, "Bearer", jwtTokenProvider.expirationSeconds(), UserResponse.from(user));
    }

    public UserResponse currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "未登录");
        }

        SysUser user = sysUserMapper.selectById(principal.id());
        if (user == null || user.getStatus() != UserStatus.ENABLED) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "登录状态已失效");
        }
        return UserResponse.from(user);
    }

    private SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
    }
}

