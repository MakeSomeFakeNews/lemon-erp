package com.lemon.erp.modules.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 受理 / 修改服务工单
 */
public record TicketRequest(
        @NotNull(message = "请选择客户") Long customerId,
        Long contactId,
        @NotBlank(message = "请选择工单类型") String type,
        @NotBlank(message = "请选择优先级") String priority,
        String channel,
        @NotBlank(message = "请填写标题") @Size(max = 128, message = "标题不能超过128个字符") String title,
        @Size(max = 2000, message = "问题描述不能超过2000个字符") String description,
        Long materialId,
        @Size(max = 64, message = "序列号不能超过64个字符") String serialNo,
        Long assigneeId) {
}
