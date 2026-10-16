package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 新增采购入库 / 采购退货 / 销售出库 / 销售退货，保存即确认
 */
/**
 * @param replenish 仅退货：退货后需要补发（销售）/ 补收（采购），订单恢复相应的未完成数量；否则视为退款，订单不再补发
 */
public record TradeDocRequest(
        @NotNull(message = "请选择订单") Long orderId,
        @NotNull(message = "请选择仓库") Long warehouseId,
        @NotNull(message = "请选择业务日期") LocalDate bizDate,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        Boolean replenish,
        @NotEmpty(message = "请添加明细") @Size(max = 500, message = "明细不能超过500行") List<@Valid @NotNull TradeDocItemRequest> items) {
}
