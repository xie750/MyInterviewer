package com.kedaxunfei.myinterviewer.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kedaxunfei.myinterviewer.common.BusinessException;
import com.kedaxunfei.myinterviewer.common.ErrorCodes;
import com.kedaxunfei.myinterviewer.domain.SysUser;
import com.kedaxunfei.myinterviewer.domain.UserRole;
import com.kedaxunfei.myinterviewer.dto.AdminUserResponse;
import com.kedaxunfei.myinterviewer.dto.UserStatusRequest;
import com.kedaxunfei.myinterviewer.repository.SysUserMapper;
import com.kedaxunfei.myinterviewer.security.AuthenticatedUser;

@Service
public class AdminUserService {

    private final SysUserMapper sysUserMapper;

    public AdminUserService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    public List<AdminUserResponse> listUsers() {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .orderByAsc(SysUser::getId))
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse updateUserStatus(AuthenticatedUser admin, Long id, UserStatusRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "用户不存在");
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "管理员账号不能在基础用户管理中启停");
        }
        if (admin.id().equals(id)) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "不能停用当前登录账号");
        }
        user.setStatus(request.status());
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return AdminUserResponse.from(sysUserMapper.selectById(id));
    }
}
