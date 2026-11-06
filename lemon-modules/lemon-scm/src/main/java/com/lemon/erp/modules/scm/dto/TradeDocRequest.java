package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 新增采购入库 / 采购退货 / 销售出库 / 销售退货，保存即确认。
 * <p>
 * 引用订单时 {@code orderId} 必填、明细按订单明细（orderItemId）录入；业务参数 scm.directDoc.enabled 开启时可不选订单直接开单：
 * 填写 {@code partnerId}，明细填写物料、单价、税率，系统生成并直接审核隐式订单（来源 DIRECT）
 *
 * @param orderId     引用的订单，直接开单时为空
 * @param warehouseId 仓库（销售出库的明细可另选仓库）
 * @param replenish   仅退货：退货后需要补发（销售）/ 补收（采购），订单恢复相应的未完成数量；否则视为退款，订单不再补发。直接开单不支持
 * @param partnerId   直接开单：客户 / 供应商
 * @param salesmanId  直接开单的销售出库：业务员（用户），为空时取客户负责人，没有时为制单人
 */
public record TradeDocRequest(
        Long orderId,
        @NotNull(message = "{scm.valid.selectWarehouse}") Long warehouseId,
        @NotNull(message = "{scm.valid.selectBusinessDate}") LocalDate bizDate,
        @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark,
        Boolean replenish,
        @NotEmpty(message = "{scm.valid.addLineItems}") @Size(max = 500, message = "{scm.valid.lineItemsCannotExceed}") List<@Valid @NotNull TradeDocItemRequest> items,
        Long partnerId,
        Long salesmanId) {

    /** 引用订单 */
    public TradeDocRequest(Long orderId, Long warehouseId, LocalDate bizDate, String remark, Boolean replenish,
                           List<TradeDocItemRequest> items) {
        this(orderId, warehouseId, bizDate, remark, replenish, items, null, null);
    }

    /** 是否直接开单（不引用订单） */
    public boolean direct() {
        return orderId == null;
    }
}
