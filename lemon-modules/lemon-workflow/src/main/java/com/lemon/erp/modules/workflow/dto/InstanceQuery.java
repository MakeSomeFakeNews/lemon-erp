package com.lemon.erp.modules.workflow.dto;

import com.lemon.erp.framework.workflow.FlowStatus;

/**
 * 流程实例查询条件
 *
 * @param name   流程标题，模糊匹配
 * @param status 流程状态
 */
public record InstanceQuery(String name, FlowStatus status) {
}
