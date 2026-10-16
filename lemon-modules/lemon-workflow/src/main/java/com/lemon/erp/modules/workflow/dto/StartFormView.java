package com.lemon.erp.modules.workflow.dto;

import com.lemon.erp.modules.workflow.model.FlowNode;
import com.lemon.erp.modules.workflow.model.FormField;

import java.util.List;

/**
 * 发起页面：最新发布版本的表单与流程
 */
public record StartFormView(String processDefinitionId, String modelKey, String name, List<FormField> form,
                            FlowNode flow) {
}
