package com.lemon.erp.modules.system.dto;

import com.lemon.erp.modules.system.entity.SysDept;

import java.util.List;

/**
 * 编辑公告时选择发布范围用的部门树和角色（不需要部门管理、角色管理的权限）
 */
public record NoticeOptions(List<SysDept> depts, List<RoleOption> roles) {

    public record RoleOption(Long id, String name, String code) {
    }
}
