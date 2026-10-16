package com.lemon.erp.modules.workflow.engine.meta;

import com.lemon.erp.modules.workflow.model.FlowBranch;
import com.lemon.erp.modules.workflow.model.FlowDesign;
import com.lemon.erp.modules.workflow.model.FlowNode;
import com.lemon.erp.modules.workflow.model.FlowTree;
import org.flowable.common.engine.api.FlowableException;

import java.util.HashMap;
import java.util.Map;

/**
 * 某个流程定义版本的设计，按 ID 索引节点和分支，供运行时快速查找
 */
public final class FlowDefinitionMeta {

    private final String processDefinitionId;
    private final FlowDesign design;
    private final Map<String, FlowNode> nodes = new HashMap<>();
    private final Map<String, FlowBranch> branches = new HashMap<>();

    public FlowDefinitionMeta(String processDefinitionId, FlowDesign design) {
        this.processDefinitionId = processDefinitionId;
        this.design = design;
        FlowTree.walk(design.flow(), n -> nodes.put(n.id(), n), b -> branches.put(b.id(), b));
    }

    public FlowDesign design() {
        return design;
    }

    /** 是否为设计中的节点或分支（BPMN 自动生成的网关、连线等返回 false） */
    public boolean isDesignElement(String id) {
        return nodes.containsKey(id) || branches.containsKey(id);
    }

    public FlowNode node(String nodeId) {
        FlowNode node = nodes.get(nodeId);
        if (node == null) {
            throw new FlowableException("流程定义 " + processDefinitionId + " 中不存在节点 " + nodeId);
        }
        return node;
    }

    public FlowBranch branch(String branchId) {
        FlowBranch branch = branches.get(branchId);
        if (branch == null) {
            throw new FlowableException("流程定义 " + processDefinitionId + " 中不存在分支 " + branchId);
        }
        return branch;
    }
}
