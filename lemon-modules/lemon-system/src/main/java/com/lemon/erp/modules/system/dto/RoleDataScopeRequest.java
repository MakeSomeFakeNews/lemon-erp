package com.lemon.erp.modules.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 分配角色数据权限
 *
 * @param dataScope 数据权限类型，见 DataScopeType
 * @param deptIds   自定义数据权限的部门，仅 dataScope = 自定义时有效
 */
public record RoleDataScopeRequest(
        @NotNull(message = "{system.valid.dataPermissionRequired}")
        @Min(value = 1, message = "{system.valid.invalidDataPermissionValue}")
        @Max(value = 5, message = "{system.valid.invalidDataPermissionValue}") Integer dataScope,
        List<Long> deptIds) {
}
