package com.lemon.erp.modules.wms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 经营历史：期初余额 + 每条流水（含结余）。没有「查看成本金额」权限时金额、单价为空
 *
 * @param openingQty    期初数量
 * @param openingAmount 期初金额
 * @param costVisible   是否返回了成本单价和金额
 * @param openingEstimated 期初金额含升级前回填的近似成本（升级前的流水 {@link StockHistoryRow#getCostEstimated()} 为 1）
 */
public record StockHistoryView(Long materialId, String materialCode, String materialName, String spec, String unit,
                               LocalDate beginDate, LocalDate endDate, BigDecimal openingQty, BigDecimal openingAmount,
                               List<StockHistoryRow> rows, boolean costVisible,
                               boolean openingEstimated) {
}
