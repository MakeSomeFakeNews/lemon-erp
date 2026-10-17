package com.lemon.erp.modules.mes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 工单领料 / 退料 / 完工入库，保存即确认
 */
public record WoDocRequest(
        @NotNull(message = "{mes.valid.selectWarehouse}") Long warehouseId,
        @NotNull(message = "{mes.valid.selectBusinessDate}") LocalDate bizDate,
        @Size(max = 255, message = "{mes.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{mes.valid.addLineItems}") @Size(max = 500, message = "{mes.valid.lineItemsCannotExceed}") List<@Valid @NotNull WoDocItemRequest> items) {
}
