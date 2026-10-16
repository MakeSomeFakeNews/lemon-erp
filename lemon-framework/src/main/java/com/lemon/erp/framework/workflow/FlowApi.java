package com.lemon.erp.framework.workflow;

import java.util.Map;

/**
 * 工作流门面，业务模块通过它把业务单据接入审批流程，不直接依赖流程引擎。
 * <p>
 * 典型用法（以订单为例）：
 * <pre>{@code
 * // 提交审批：流程变量可以在流程设计的条件分支中使用
 * String instanceId = flowApi.start("order_approve", "order:" + order.getId(),
 *         Map.of("amount", order.getAmount()));
 *
 * // 监听审批结果，回写单据状态（与审批操作在同一事务中）
 * @EventListener(condition = "#e.processKey == 'order_approve'")
 * public void onFinished(FlowFinishedEvent e) { ... }
 * }</pre>
 */
public interface FlowApi {

    /**
     * 以当前登录用户的身份发起流程（使用该流程最新发布的版本）
     *
     * @param processKey  流程标识（流程模型的 modelKey）
     * @param businessKey 业务标识，建议「业务类型:业务主键」，审批结束事件中原样返回
     * @param variables   流程变量，条件分支可引用；为 null 时不传
     * @return 流程实例 ID
     */
    String start(String processKey, String businessKey, Map<String, Object> variables);

    /**
     * 流程是否可以发起：模型存在、已启用且已发布。业务模块据此决定单据走审批流程还是直接审核
     */
    boolean isAvailable(String processKey);

    /**
     * 撤销运行中的流程，状态变为 {@link FlowStatus#CANCELED}。不校验是否为发起人，撤销人记为当前登录用户
     *
     * @param reason 撤销原因，记录到审批记录中
     */
    void cancel(String processInstanceId, String reason);

    /**
     * @return 流程状态，流程实例不存在时返回 null
     */
    FlowStatus getStatus(String processInstanceId);
}
