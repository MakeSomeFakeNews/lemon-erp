package com.lemon.erp.framework.workflow;

/**
 * 流程结束事件（通过、拒绝、撤回、终止），在流程引擎的事务中同步发布：
 * 用 {@code @EventListener} 监听时与审批操作同一事务，监听器抛异常会回滚审批；
 * 只需在提交后处理（发通知等）时用 {@code @TransactionalEventListener}。
 *
 * @param processKey  流程标识，用于区分是哪个业务的流程
 * @param businessKey 发起时传入的业务标识，页面发起的流程为 null
 * @param startUserId 发起人
 */
public record FlowFinishedEvent(String processInstanceId, String processKey, String businessKey,
                                FlowStatus status, Long startUserId) {
}
