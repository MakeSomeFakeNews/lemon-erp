package com.lemon.erp.modules.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 受理 / 修改服务工单
 */
public record TicketRequest(
        @NotNull(message = "{crm.valid.selectCustomer}") Long customerId,
        Long contactId,
        @NotBlank(message = "{crm.valid.selectTicketType}") String type,
        @NotBlank(message = "{crm.valid.selectPriority}") String priority,
        String channel,
        @NotBlank(message = "{crm.valid.enterTitle}") @Size(max = 128, message = "{crm.valid.titleCannotExceedCharacters2}") String title,
        @Size(max = 2000, message = "{crm.valid.descriptionCannotExceedCharacters2}") String description,
        Long materialId,
        @Size(max = 64, message = "{crm.valid.serialNoCannotExceedCharacters}") String serialNo,
        Long assigneeId) {
}
