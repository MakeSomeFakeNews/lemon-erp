package com.lemon.erp.modules.workflow.engine.assignee;

import com.lemon.erp.modules.workflow.model.AssigneeConfig;

/**
 * 计算审批人 / 抄送人时可用的信息，与流程引擎 API 解耦，方便策略单独测试
 *
 * @param initiatorId 发起人
 */
public record AssigneeContext(AssigneeConfig config, Long initiatorId) {
}
