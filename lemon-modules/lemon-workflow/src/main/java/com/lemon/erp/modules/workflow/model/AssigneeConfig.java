package com.lemon.erp.modules.workflow.model;

import java.util.List;

/**
 * 审批人 / 抄送人配置
 *
 * @param ids   用户 ID 或角色 ID，取决于 type
 * @param names 对应的名称，只用于设计器回显，运行时以 ids 为准
 * @param level 部门主管的级别，DEPT_LEADER 使用：1 为发起人所在部门的主管，2 为上一级部门的主管，依此类推
 */
public record AssigneeConfig(AssigneeType type, List<Long> ids, List<String> names, Integer level) {

    public static final int MAX_LEVEL = 10;

    public AssigneeConfig {
        ids = ids == null ? List.of() : ids;
    }

    public int levelOrDefault() {
        return level == null ? 1 : level;
    }
}
