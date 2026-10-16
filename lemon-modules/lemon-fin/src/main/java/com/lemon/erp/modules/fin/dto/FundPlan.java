package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.FundSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 资金计划：从现金及银行存款的当前余额出发，按期汇总预计流入、流出，计算各期期末余额
 *
 * @param opening 当前余额
 * @param periods 第一期为「已逾期」（到期日早于今天、仍未收付的），之后按周 / 月
 */
public record FundPlan(BigDecimal opening, List<Period> periods) {

    /**
     * @param inflow   各来源的流入
     * @param outflow  各来源的流出
     * @param shortage 期末余额为负（资金缺口）
     */
    public record Period(String label, LocalDate start, LocalDate end, Map<FundSource, BigDecimal> inflow,
                         Map<FundSource, BigDecimal> outflow, BigDecimal totalIn, BigDecimal totalOut, BigDecimal closing,
                         boolean shortage) {
    }
}
