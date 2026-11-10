package com.lemon.erp.framework.workflow.identity;

import com.lemon.erp.common.result.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 工作流所需的组织架构查询，由系统模块实现，工作流模块不直接依赖系统模块的表
 */
public interface FlowIdentityProvider {

    /** 过滤出存在且启用的内部用户（排除供应商 / 经销商门户账号，门户账号不能审批），保持传入顺序 */
    List<Long> listEnabledUserIds(Collection<Long> userIds);

    /** 拥有任一角色（角色启用）的启用内部用户 */
    List<Long> listUserIdsByRoleIds(Collection<Long> roleIds);

    /** 拥有指定角色编码（角色启用）的启用内部用户 */
    List<Long> listUserIdsByRoleCode(String roleCode);

    /**
     * 用户所在部门及其全部上级部门，由近到远（第一个为用户所在部门）；用户没有部门时返回空列表
     */
    List<FlowDept> listDeptChain(Long userId);

    /** 用户显示名（昵称，没有昵称时为用户名），不存在的用户不返回 */
    Map<Long, String> getUserNames(Collection<Long> userIds);

    /** 按用户名或昵称搜索启用的内部用户（排除门户账号），用于选择审批人、转交，以及各业务表单的选人（{@code GET /workflow/identity/users}） */
    PageResult<FlowUser> searchUsers(String keyword, long current, long size);

    /** 启用的角色，用于按角色指定审批人 */
    List<FlowRole> listRoles();
}
