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
        @NotNull(message = "请选择仓库") Long warehouseId,
        @NotNull(message = "请选择业务日期") LocalDate bizDate,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        @NotEmpty(message = "请添加明细") @Size(max = 500, message = "明细不能超过500行") List<@Valid @NotNull WoDocItemRequest> items) {
}
