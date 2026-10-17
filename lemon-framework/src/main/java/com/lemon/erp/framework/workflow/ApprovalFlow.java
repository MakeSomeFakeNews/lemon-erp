package com.lemon.erp.framework.workflow;

import java.util.function.BiConsumer;

/**
 * 单据接入审批的声明：流程标识 + 业务标识前缀。业务标识为「前缀 + 单据 ID」，审批结束事件中原样返回，据此找回单据。
 * 在业务 Service 中声明为常量，配合 {@link ApprovalSupport#submit} 发起、{@link #onFinished} 处理审批结果：
 * <pre>{@code
 * private static final ApprovalFlow FLOW = new ApprovalFlow("crm_contract", "crm_contract:");
 *
 * @EventListener
 * public void onFlowFinished(FlowFinishedEvent e) {
 *     FLOW.onFinished(e, (id, approved) -> { if (approved) activate(id); else backToDraft(id); });
 * }
 * }</pre>
 *
 * @param processKey 流程标识（流程模型的 modelKey，与 FlowBusiness 声明一致）
 * @param keyPrefix  业务标识前缀（含分隔符，如 {@code "crm_contract:"}）；已发起的流程按原前缀回调，不能修改
 */
public record ApprovalFlow(String processKey, String keyPrefix) {

    public String businessKey(Long id) {
        return keyPrefix + id;
    }

    /** 审批结束事件对应的单据 ID，不是本流程的单据时返回 null */
    public Long idOf(FlowFinishedEvent e) {
        String key = e.businessKey();
        if (key == null || !key.startsWith(keyPrefix)) {
            return null;
        }
        return Long.valueOf(key.substring(keyPrefix.length()));
    }

    /**
     * 是本流程的单据时处理审批结果
     *
     * @param handler (单据 ID, 是否通过)；驳回、撤销、终止都为 false
     */
    public void onFinished(FlowFinishedEvent e, BiConsumer<Long, Boolean> handler) {
        Long id = idOf(e);
        if (id != null) {
            handler.accept(id, e.status() == FlowStatus.APPROVED);
        }
    }

    /**
     * 是本流程的单据时处理审批结果，需要区分驳回与撤销等情况时使用
     *
     * @param handler (单据 ID, 流程结束状态)
     */
    public void onFinishedWithStatus(FlowFinishedEvent e, BiConsumer<Long, FlowStatus> handler) {
        Long id = idOf(e);
        if (id != null) {
            handler.accept(id, e.status());
        }
    }
}
