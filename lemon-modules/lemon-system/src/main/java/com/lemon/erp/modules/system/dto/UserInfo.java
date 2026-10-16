package com.lemon.erp.modules.system.dto;

import com.lemon.erp.modules.system.entity.SysUser;

import java.util.Set;

/**
 * 当前用户信息 + 角色 + 权限标识，前端用于按钮级权限控制
 *
 * @param tenantName SaaS 模式下当前租户的名称，未开启时为 null
 */
public record UserInfo(SysUser user, Set<String> roles, Set<String> perms, String tenantName) {
}
