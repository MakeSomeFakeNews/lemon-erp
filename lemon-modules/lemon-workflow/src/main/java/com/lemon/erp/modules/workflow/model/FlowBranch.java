package com.lemon.erp.modules.workflow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * 分支
 *
 * @param defaultBranch 是否默认分支（条件分支中其它分支都不满足时进入，固定在最后）
 * @param conditions    分支条件：组与组之间是「或」，组内规则之间是「且」；默认分支和并行分支为空
 * @param next          分支内的第一个节点，为空表示分支内没有节点
 */
public record FlowBranch(String id, String name, Boolean defaultBranch, List<List<ConditionRule>> conditions,
                         FlowNode next) {

    public FlowBranch {
        conditions = conditions == null ? List.of() : conditions;
    }

    /** 是否默认分支（未传按非默认处理） */
    @JsonIgnore
    public boolean isDefaultPath() {
        return Boolean.TRUE.equals(defaultBranch);
    }
}
