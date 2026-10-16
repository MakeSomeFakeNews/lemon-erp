package com.lemon.erp.modules.workflow.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 待办 / 已办列表项
 *
 * @param processName 流程标题
 * @param startTime   流程发起时间
 * @param createTime  任务到达时间
 * @param endTime     任务处理时间，仅已办
 * @param action      处理结果 APPROVE / REJECT，仅已办
 * @param status      流程当前状态，仅已办
 */
public record TaskItem(String taskId, String taskName, String nodeId, String processInstanceId, String processName,
                       String processKey, Long startUserId, String startUserName,
                       @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                       @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime,
                       @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                       String action, String status) {
}
