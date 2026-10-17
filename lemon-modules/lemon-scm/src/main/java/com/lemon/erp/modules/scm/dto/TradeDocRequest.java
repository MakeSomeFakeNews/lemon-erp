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
        @NotNull(message = "{scm.valid.selectOrder}") Long orderId,
        @NotNull(message = "{scm.valid.selectWarehouse}") Long warehouseId,
        @NotNull(message = "{scm.valid.selectBusinessDate}") LocalDate bizDate,
        @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark,
        Boolean replenish,
        @NotEmpty(message = "{scm.valid.addLineItems}") @Size(max = 500, message = "{scm.valid.lineItemsCannotExceed}") List<@Valid @NotNull TradeDocItemRequest> items) {
}
