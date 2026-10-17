package com.lemon.erp.modules.scm.dto;

import com.lemon.erp.modules.scm.model.SupplierLevel;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 供应商绩效：按采购业务数据统计，供评估打分参考
 */
@Data
public class SupplierKpi {

    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private SupplierLevel level;

    /** 采购订单数（已审核及以后） */
    private Long orderCount;

    /** 采购入库金额（扣除退货） */
    private BigDecimal purchaseAmount;

    /** 入库明细行数 */
    private Long inLines;

    /** 按期入库的明细行数：入库日期不晚于订单明细的交货日期（未填交货日期视为按期） */
    private Long onTimeLines;

    private BigDecimal inQty;

    private BigDecimal returnQty;

    /** 来料检验批次、合格批次（让步接收计为合格），由生产模块的质量检验统计 */
    private Long iqcBatches;
    private Long iqcPassBatches;

    /** 已确认的质量索赔次数、金额，由质量管理统计 */
    private Long claimCount;
    private BigDecimal claimAmount;

    /** 最近一次评估 */
    private BigDecimal lastScore;
    private SupplierLevel lastGrade;
    private String lastPeriod;

    /** 准时交货率（%） */
    public BigDecimal getOnTimeRate() {
        return rate(onTimeLines == null ? null : BigDecimal.valueOf(onTimeLines), inLines == null ? null : BigDecimal.valueOf(inLines));
    }

    /** 来料批次合格率（%） */
    public BigDecimal getIqcPassRate() {
        return rate(iqcPassBatches == null ? null : BigDecimal.valueOf(iqcPassBatches), iqcBatches == null ? null : BigDecimal.valueOf(iqcBatches));
    }

    /** 退货率（%）= 退货数量 / 入库数量 */
    public BigDecimal getReturnRate() {
        return rate(returnQty, inQty);
    }

    private static BigDecimal rate(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.signum() == 0) {
            return null;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 1, java.math.RoundingMode.HALF_UP);
    }
}
