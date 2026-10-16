package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.system.entity.SysRole;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.entity.SysUserRole;
import com.lemon.erp.modules.system.mapper.SysRoleMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import com.lemon.erp.modules.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 角色成员管理：从角色的角度查看、分配、移除用户
 */
@Service
@RequiredArgsConstructor
public class SysRoleUserService {

    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserService userService;
    private final TokenService tokenService;

    /**
     * 分页查询角色的成员（assigned = true）或可添加的用户（assigned = false），只返回数据权限范围内的用户
     */
    @DataPermission(table = "sys_user", deptColumn = "dept_id", userColumn = "id")
    public PageResult<SysUser> pageUsers(Long roleId, boolean assigned, String username, long current, long size) {
        getRole(roleId);
        Page<SysUser> page = Page.of(current, size);
        userMapper.selectPageByRole(page, roleId, assigned, username);
        userService.fillDeptName(page.getRecords());
        return PageResult.of(page);
    }

    @Transactional
    public void addUsers(Long roleId, List<Long> userIds) {
        SysRole role = getRole(roleId);
        checkRoleAssignable(role);
        userService.checkOperable(userIds);
        Set<Long> existing = new HashSet<>();
        userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getRoleId, roleId).in(SysUserRole::getUserId, userIds))
                .forEach(ur -> existing.add(ur.getUserId()));
        List<Long> toAdd = userIds.stream().distinct().filter(id -> !existing.contains(id)).toList();
        if (!toAdd.isEmpty()) {
            userRoleMapper.insertBatchByRole(roleId, toAdd);
        }
        toAdd.forEach(tokenService::evictLoginUser);
    }

    @Transactional
    public void removeUsers(Long roleId, List<Long> userIds) {
        SysRole role = getRole(roleId);
        checkRoleAssignable(role);
        if (PermissionService.ADMIN_ROLE.equals(role.getCode()) && userIds.contains(SysUserService.SUPER_ADMIN_ID)) {
            throw new BizException("不允许移除内置超级管理员的超级管理员角色");
        }
        userService.checkOperable(userIds);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getRoleId, roleId).in(SysUserRole::getUserId, userIds));
        userIds.forEach(tokenService::evictLoginUser);
    }

    private SysRole getRole(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException("角色不存在");
        }
        return role;
    }

    /** 只有超级管理员能增减超级管理员角色的成员 */
    private void checkRoleAssignable(SysRole role) {
        if (PermissionService.ADMIN_ROLE.equals(role.getCode()) && !SecurityUtils.getLoginUser().isAdmin()) {
            throw new BizException("不允许分配超级管理员角色");
        }
    }
}
