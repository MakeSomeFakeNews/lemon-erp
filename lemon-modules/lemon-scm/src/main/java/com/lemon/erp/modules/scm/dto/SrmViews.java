package com.lemon.erp.modules.scm.dto;

import java.math.BigDecimal;

/**
 * 采购合同、招标、供应商协同的查询结果
 */
public final class SrmViews {

    private SrmViews() {
    }

    /**
     * 合同物料的执行情况：非草稿的引用订单（关闭的订单按已入库数量）
     *
     * @param receivedQty 已入库（扣除退货）
     */
    public record ContractUsage(Long materialId, BigDecimal orderedQty, BigDecimal orderedAmount, BigDecimal receivedQty) {
    }

    /** 采购订单明细的未送货数量：订单数量 - 已入库 - 待收货的送货通知 */
    public record AsnOpenLine(Long orderItemId, BigDecimal openQty) {
    }
}
