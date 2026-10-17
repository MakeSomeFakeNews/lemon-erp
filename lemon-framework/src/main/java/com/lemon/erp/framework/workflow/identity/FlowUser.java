package com.lemon.erp.framework.workflow.identity;

import com.lemon.erp.common.enums.EnableStatus;


/**
 * 工作流中展示、选择的用户
 */
public record FlowUser(Long id, String username, String nickname, String deptName, String avatar, EnableStatus status) {
}
