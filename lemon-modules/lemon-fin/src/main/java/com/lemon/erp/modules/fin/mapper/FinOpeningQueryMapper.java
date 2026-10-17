package com.lemon.erp.modules.fin.mapper;

import java.util.List;
import java.util.Map;

/**
 * 启用向导的统计：期初入库单、手工录入的期初往来
 */
public interface FinOpeningQueryMapper {

    /** 期初入库单：[{id, docNo, bizDate, warehouseName, totalQty, reversed}] */
    List<Map<String, Object>> selectOpeningStockDocs();

    /** 手工录入的应收应付（期初往来）按种类汇总：[{kind, count, amount}] */
    List<Map<String, Object>> selectManualBillSums();
}
