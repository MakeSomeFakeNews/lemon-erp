package com.lemon.erp.modules.workflow.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.workflow.model.ApprovalAction;

import java.time.LocalDateTime;

/**
 * 审批记录
 *
 * @param userName 操作人；抄送时为全部抄送人，用「、」分隔
 * @param time     操作时间，待审批时为 null
 */
public record ApprovalRecord(String nodeId, String nodeName, ApprovalAction action, Long userId, String userName,
                             String comment, @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime time) {
}
