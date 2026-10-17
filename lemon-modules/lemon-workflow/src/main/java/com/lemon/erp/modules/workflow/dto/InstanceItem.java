package com.lemon.erp.modules.workflow.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.framework.workflow.FlowStatus;

import java.time.LocalDateTime;

/**
 * 流程实例列表项
 *
 * @param name         流程标题，如「张三的采购申请」
 * @param processName  流程名称（流程定义名称）
 * @param category     流程分类（字典 wf_category）
 * @param currentNodes 运行中的节点名称，多个用「、」分隔
 */
public record InstanceItem(String id, String name, String processKey, String processName, String category,
                           FlowStatus status, Long startUserId, String startUserName,
                           @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                           @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                           String currentNodes) {
}
