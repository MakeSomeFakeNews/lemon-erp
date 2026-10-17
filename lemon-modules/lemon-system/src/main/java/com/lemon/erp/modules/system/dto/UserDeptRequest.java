package com.lemon.erp.modules.system.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量调整用户部门
 */
public record UserDeptRequest(
        @NotEmpty(message = "{system.valid.selectUsers}") @Size(max = 500, message = "{system.valid.upUsersProcessedTime}") List<Long> userIds,
        @NotNull(message = "{system.valid.selectDepartment}") Long deptId) {
}
