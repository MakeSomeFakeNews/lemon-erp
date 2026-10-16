package com.lemon.erp.framework.workflow.identity;

/**
 * 工作流中用到的部门信息
 *
 * @param leaderUserId 部门负责人，未设置为 null
 */
public record FlowDept(Long id, String name, Long parentId, Long leaderUserId) {
}
