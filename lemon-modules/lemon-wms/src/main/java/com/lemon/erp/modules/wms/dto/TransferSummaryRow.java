package com.lemon.erp.modules.wms.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 调拨汇总的一行：调出仓库 → 调入仓库（在途调拨的退回、损耗不算调出）。调拨差价 = 调拨金额 − 调出成本金额（内部结算口径，不影响存货成本）
 */
@Data
public class TransferSummaryRow {

    private Long warehouseId;
    private String warehouseName;
    private Long toWarehouseId;
    private String toWarehouseName;
    /** 调拨单数（不含红字冲销单） */
    private Integer docCount;
    /** 调出数量（在途调拨扣除退回调出仓、确认损耗的数量） */
    private BigDecimal totalQty;
    private BigDecimal costAmount;
    private BigDecimal transferAmount;
    /** 在途数量：在途调拨尚未验收、退回或损耗的数量（含差异待处理） */
    private BigDecimal transitQty;
    /** 在途调拨退回调出仓、确认损耗的数量（已从调出数量中扣除） */
    private BigDecimal backQty;
    /** 调拨费用 */
    private BigDecimal fee;

    /** 调拨差价 */
    public BigDecimal getDiffAmount() {
        return transferAmount == null || costAmount == null ? null : transferAmount.subtract(costAmount);
    }
}
