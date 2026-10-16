package com.lemon.erp.framework.workflow;

/**
 * 审批任务分配给某个用户（新任务生成或转交），在流程引擎的事务中同步发布，
 * 可用于发送待办提醒；建议用 {@code @TransactionalEventListener} 在事务提交后处理
 *
 * @param processName 流程实例标题，如「张三的采购申请」
 * @param taskName    节点名称
 */
public record FlowTaskAssignedEvent(String taskId, String taskName, String processInstanceId, String processKey,
                                    String processName, Long assigneeId) {
}
