package com.lemon.erp.modules.fin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.modules.fin.model.BillKind;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 期初建账概况：科目期初、期初入库单、期初库存成本、期初应收应付
 *
 * @param bills 按应收 / 应付
 */
public record OpeningSummary(AccountTotals accounts, List<StockDoc> stockDocs, StockStatus stock, Map<BillKind, BillTotals> bills) {

    /** 科目期初：条数、借方合计、贷方合计 */
    public record AccountTotals(int count, BigDecimal debit, BigDecimal credit) {
    }

    /** 期初入库单（数据库查询结果，为空的列不输出） */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StockDoc {
        private Long id;
        private String docNo;
        private LocalDate bizDate;
        private String warehouseName;
        private BigDecimal totalQty;
        private Integer reversed;
    }

    /**
     * 期初库存成本
     *
     * @param saved         已保存期初成本
     * @param openingPeriod 期初成本所在期间
     * @param costed        已做过成本核算（之后不能再改期初）
     */
    public record StockStatus(boolean saved, Integer openingPeriod, boolean costed) {
    }

    /**
     * 期初应收 / 应付
     *
     * @param count       手工录入的期初单据数
     * @param amount      手工录入的期初单据金额
     * @param accountCode 应收 / 应付账款科目
     * @param ledger      该科目的期初余额
     */
    public record BillTotals(int count, BigDecimal amount, String accountCode, BigDecimal ledger) {
    }

    /** 手工录入的应收应付单按种类汇总（数据库查询结果） */
    @Data
    public static class ManualBillSum {
        private BillKind kind;
        private Long count;
        private BigDecimal amount;
    }
}
