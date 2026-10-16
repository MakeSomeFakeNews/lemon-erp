package com.lemon.erp.modules.workflow.dto;

/**
 * 流程实例查询条件
 *
 * @param name   流程标题，模糊匹配
 * @param status 流程状态，见 FlowStatus
 */
public record InstanceQuery(String name, String status) {
}
