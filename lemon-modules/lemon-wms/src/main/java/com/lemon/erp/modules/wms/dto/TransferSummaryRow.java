package com.lemon.erp.modules.wms.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 调拨汇总的一行：调出仓库 → 调入仓库。调拨差价 = 调拨金额 − 调出成本金额（内部结算口径，不影响存货成本）
 */
@Data
public class TransferSummaryRow {

    private Long warehouseId;
    private String warehouseName;
    private Long toWarehouseId;
    private String toWarehouseName;
    /** 调拨单数（不含红字冲销单） */
    private Integer docCount;
    private BigDecimal totalQty;
    private BigDecimal costAmount;
    private BigDecimal transferAmount;
    /** 调拨费用 */
    private BigDecimal fee;

    /** 调拨差价 */
    public BigDecimal getDiffAmount() {
        return transferAmount == null || costAmount == null ? null : transferAmount.subtract(costAmount);
    }
}
