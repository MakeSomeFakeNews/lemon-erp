package com.lemon.erp.modules.fin.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 科目余额表：各科目行 + 一级科目合计（期初、期末按借贷方向分列，用于试算平衡）
 *
 * @param balanced 期初、本期、期末借贷是否都平衡
 */
public record TrialBalance(List<BalanceRow> rows, BigDecimal openDebit, BigDecimal openCredit, BigDecimal debit,
                           BigDecimal credit, BigDecimal closeDebit, BigDecimal closeCredit, boolean balanced) {

    public static TrialBalance of(List<BalanceRow> rows, BigDecimal[] t) {
        boolean balanced = t[0].compareTo(t[1]) == 0 && t[2].compareTo(t[3]) == 0 && t[4].compareTo(t[5]) == 0;
        return new TrialBalance(rows, t[0], t[1], t[2], t[3], t[4], t[5], balanced);
    }
}
