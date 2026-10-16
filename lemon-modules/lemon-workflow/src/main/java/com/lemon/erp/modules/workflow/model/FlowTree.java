package com.lemon.erp.modules.workflow.model;

import java.util.function.Consumer;

/**
 * 流程节点树的遍历
 */
public final class FlowTree {

    private FlowTree() {
    }

    /**
     * 深度优先遍历 node 及其后续节点、各分支内的节点
     */
    public static void walk(FlowNode node, Consumer<FlowNode> nodeVisitor, Consumer<FlowBranch> branchVisitor) {
        for (FlowNode current = node; current != null; current = current.next()) {
            nodeVisitor.accept(current);
            for (FlowBranch branch : current.branches()) {
                branchVisitor.accept(branch);
                walk(branch.next(), nodeVisitor, branchVisitor);
            }
        }
    }
}
