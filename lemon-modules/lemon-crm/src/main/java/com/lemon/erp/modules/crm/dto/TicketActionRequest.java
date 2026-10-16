package com.lemon.erp.modules.crm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 工单处理：指派（assigneeId）、处理记录 / 解决方案 / 重开原因（content）、关闭时的满意度（satisfaction）
 */
public record TicketActionRequest(
        Long assigneeId,
        @Size(max = 2000, message = "内容不能超过2000个字符") String content,
        @Min(value = 1, message = "满意度为1-5") @Max(value = 5, message = "满意度为1-5") Integer satisfaction) {
}
