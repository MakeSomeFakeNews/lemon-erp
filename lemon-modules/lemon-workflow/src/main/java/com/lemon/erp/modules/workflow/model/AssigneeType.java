package com.lemon.erp.modules.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批人类型，每种类型对应一个 AssigneeStrategy 实现。
 * 新增类型：在这里加一个枚举值，再实现一个 AssigneeStrategy 的 Spring Bean 即可
 */
@Getter
@AllArgsConstructor
public enum AssigneeType {

    USER("指定成员", true),  // i18n-ignore 未输出给前端，前端常量 ASSIGNEE_TYPES 翻译
    ROLE("指定角色", true),  // i18n-ignore 未输出给前端，前端常量 ASSIGNEE_TYPES 翻译
    /** 发起人的部门主管，按 AssigneeConfig.level 指定级别 */
    DEPT_LEADER("部门主管", false),  // i18n-ignore 未输出给前端，前端常量 ASSIGNEE_TYPES 翻译
    INITIATOR("发起人自己", false);  // i18n-ignore 未输出给前端，前端常量 ASSIGNEE_TYPES 翻译

    private final String label;

    /** 是否需要在 ids 中指定具体对象 */
    private final boolean requiresIds;
}
