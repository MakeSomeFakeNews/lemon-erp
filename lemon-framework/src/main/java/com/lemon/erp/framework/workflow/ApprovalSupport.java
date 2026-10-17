package com.lemon.erp.framework.workflow;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 单据提交审批的统一流程：流程未发布时直接审核；已发布时先把单据改为审批中，再发起流程并记录流程实例 ID。
 * 调用方在自己的事务中调用（单据状态与流程一起提交或回滚）
 */
@Component
@RequiredArgsConstructor
public class ApprovalSupport {

    private final FlowApi flowApi;

    /**
     * @param variables      流程变量（与 FlowBusiness 声明一致）
     * @param direct         流程未发布时执行：直接审核
     * @param toApproving    发起前执行：单据改为审批中（按原状态条件更新）
     * @param saveInstanceId 发起后执行：记录流程实例 ID
     * @return true 已发起审批，false 已直接审核
     */
    public boolean submit(ApprovalFlow flow, Long id, Map<String, Object> variables,
                          Runnable direct, Runnable toApproving, Consumer<String> saveInstanceId) {
        if (!flowApi.isAvailable(flow.processKey())) {
            direct.run();
            return false;
        }
        toApproving.run();
        saveInstanceId.accept(flowApi.start(flow.processKey(), flow.businessKey(id), variables));
        return true;
    }

    /**
     * 同 {@link #submit(ApprovalFlow, Long, Map, Runnable, Runnable, Consumer)}，流程变量只在需要发起审批时才计算（变量需要查询时使用）
     */
    public boolean submit(ApprovalFlow flow, Long id, Supplier<Map<String, Object>> variables,
                          Runnable direct, Runnable toApproving, Consumer<String> saveInstanceId) {
        if (!flowApi.isAvailable(flow.processKey())) {
            direct.run();
            return false;
        }
        toApproving.run();
        saveInstanceId.accept(flowApi.start(flow.processKey(), flow.businessKey(id), variables.get()));
        return true;
    }

    /**
     * 直接发起审批（调用方已确认流程已发布，如只有满足条件时才走审批的单据）：改为审批中 → 发起流程 → 记录流程实例 ID
     */
    public void start(ApprovalFlow flow, Long id, Map<String, Object> variables, Runnable toApproving, Consumer<String> saveInstanceId) {
        toApproving.run();
        saveInstanceId.accept(flowApi.start(flow.processKey(), flow.businessKey(id), variables));
    }

    /**
     * 撤回审批中的单据：流程仍在运行时撤销（会触发审批结束事件，状态为 CANCELED）
     *
     * @param reason 撤销原因，记录到审批记录中
     * @return 是否撤销了流程（流程实例不存在或已结束时返回 false）
     */
    public boolean cancelIfRunning(String instanceId, String reason) {
        if (instanceId == null || flowApi.getStatus(instanceId) != FlowStatus.RUNNING) {
            return false;
        }
        flowApi.cancel(instanceId, reason);
        return true;
    }

    /** 流程是否已发布（单据据此决定走审批还是直接审核） */
    public boolean isAvailable(ApprovalFlow flow) {
        return flowApi.isAvailable(flow.processKey());
    }
}
