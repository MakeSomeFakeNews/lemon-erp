package com.lemon.erp.modules.workflow.model;

import java.util.List;

/**
 * 流程设计：表单 + 流程节点树。保存在流程模型中，发布时随 BPMN 一起部署，
 * 每个流程定义版本都保留自己的设计，运行中的流程不受后续修改影响
 */
public record FlowDesign(List<FormField> form, FlowNode flow) {

    public FlowDesign {
        form = form == null ? List.of() : form;
    }
}
