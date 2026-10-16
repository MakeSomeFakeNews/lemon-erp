package com.lemon.erp.modules.workflow.model;

import java.util.List;

/**
 * 流程设计器节点（仿钉钉的树形结构）：每个节点通过 next 串联下一个节点，
 * 分支节点的各分支在 branches 中，分支汇合后继续执行 next
 *
 * @param assignee    审批人 / 抄送人，APPROVER、CC 节点使用
 * @param approveMode 多人审批方式，APPROVER 节点使用
 * @param emptyAction 审批人为空时的处理方式，APPROVER 节点使用
 * @param branches    分支，CONDITION、PARALLEL 节点使用
 */
public record FlowNode(String id, NodeType type, String name, AssigneeConfig assignee, ApproveMode approveMode,
                       EmptyAction emptyAction, List<FlowBranch> branches, FlowNode next) {

    public FlowNode {
        branches = branches == null ? List.of() : branches;
    }

    public ApproveMode approveModeOrDefault() {
        return approveMode == null ? ApproveMode.SEQUENTIAL : approveMode;
    }

    public EmptyAction emptyActionOrDefault() {
        return emptyAction == null ? EmptyAction.AUTO_PASS : emptyAction;
    }
}
