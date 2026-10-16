package com.lemon.erp.modules.workflow.engine.assignee;

import com.lemon.erp.modules.workflow.model.AssigneeType;

import java.util.List;

/**
 * 审批人计算策略，每种 {@link AssigneeType} 一个实现，注册为 Spring Bean 后自动生效。
 * 扩展示例：部门主管、发起人自选、表单中的人员字段等
 */
public interface AssigneeStrategy {

    AssigneeType type();

    /**
     * @return 候选用户 ID，可以包含停用的用户（由调用方统一过滤），顺序即依次审批的顺序
     */
    List<Long> resolve(AssigneeContext context);
}
