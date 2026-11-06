package com.lemon.erp.modules.fin.ai;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiReportMetrics;
import com.lemon.erp.framework.ai.AiReportPeriod;
import com.lemon.erp.framework.ai.AiReportSource;
import com.lemon.erp.modules.fin.dto.AgingRow;
import com.lemon.erp.modules.fin.dto.CashierViews;
import com.lemon.erp.modules.fin.dto.IncomeTrendRow;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.service.FinBillService;
import com.lemon.erp.modules.fin.service.FinCashierService;
import com.lemon.erp.modules.fin.service.FinFinancialStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能报告「财务」：复用财务的统计——
 * <ul>
 *   <li>利润（只有整月期间）：利润表各月趋势（{@link FinFinancialStatementService#incomeTrend}，已记账凭证）的本月、上月、去年同月</li>
 *   <li>资金：资金日报（{@link FinCashierService#daily}，现金、银行科目，含未记账凭证）的期初、收入、支出、期末，上期的收支</li>
 *   <li>应收应付：账龄分析（{@link FinBillService#aging}）在期末与上期末的余额、逾期、90 天以上，余额最大的往来单位</li>
 * </ul>
 * 财务数据是全公司的，只对有「全部数据」权限的用户开放
 */
@Component
@RequiredArgsConstructor
public class FinFinanceReportSource implements AiReportSource {

    private static final int PAGE_SIZE = 500;
    private static final int PAGE_LIMIT = 20;
    private static final int TOP = 5;

    private final FinFinancialStatementService statementService;
    private final FinCashierService cashierService;
    private final FinBillService billService;

    @Override
    public String key() {
        return "fin.finance";
    }

    @Override
    public String labelKey() {
        return "fin.ai.report.finance";
    }

    @Override
    public int order() {
        return 60;
    }

    @Override
    public String permission() {
        return "fin:report:list";
    }

    @Override
    public boolean requiresAllData() {
        return true;
    }

    @Override
    public FinanceSnapshot snapshot(AiReportPeriod period) {
        AiReportPeriod prev = period.previous();
        CashierViews.DailyTotal cash = cashierService.daily(period.start(), period.end(), false).total();
        CashierViews.DailyTotal prevCash = cashierService.daily(prev.start(), prev.end(), false).total();
        Cash funds = new Cash(cash.open(), cash.debit(), cash.credit(), cash.close(), cash.debit().subtract(cash.credit()),
                prevCash.debit(), prevCash.credit(), AiReportMetrics.growth(cash.debit(), prevCash.debit()),
                AiReportMetrics.growth(cash.credit(), prevCash.credit()));
        return new FinanceSnapshot(profit(period), funds, balance(BillKind.AR, period.end(), prev.end()),
                balance(BillKind.AP, period.end(), prev.end()));
    }

    /** 利润表按月结账，只有整月的期间才有利润数据 */
    private Profit profit(AiReportPeriod period) {
        YearMonth ym = YearMonth.from(period.start());
        if (!period.start().equals(ym.atDay(1)) || !period.end().equals(ym.atEndOfMonth())) {
            return null;
        }
        IncomeTrendRow current = month(ym);
        IncomeTrendRow previous = month(ym.minusMonths(1));
        IncomeTrendRow lastYear = month(ym.minusYears(1));
        return new Profit(current, previous, lastYear, AiReportMetrics.growth(current.revenue(), previous.revenue()),
                AiReportMetrics.growth(current.revenue(), lastYear.revenue()), AiReportMetrics.growth(current.net(), previous.net()),
                AiReportMetrics.percent(current.net(), current.revenue()));
    }

    private IncomeTrendRow month(YearMonth ym) {
        List<IncomeTrendRow> rows = statementService.incomeTrend(ym.getYear() * 100 + ym.getMonthValue());
        return rows.get(rows.size() - 1);
    }

    /** 账龄分析在 asOf 的合计（逐页汇总）与余额最大的往来单位 */
    Balance balance(BillKind kind, LocalDate asOf, LocalDate previousAsOf) {
        BigDecimal[] now = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        List<Partner> top = new ArrayList<>();
        long partners = 0;
        for (int page = 1; page <= PAGE_LIMIT; page++) {
            PageResult<AgingRow> result = billService.aging(page, PAGE_SIZE, kind, asOf);
            for (AgingRow r : result.records()) {
                BigDecimal balance = Decimals.nz(r.getBalance());
                BigDecimal overdue = balance.subtract(Decimals.nz(r.getNotDue()));
                now[0] = now[0].add(balance);
                now[1] = now[1].add(overdue);
                now[2] = now[2].add(Decimals.nz(r.getOver90()));
                partners++;
                // 账龄分析按余额降序，前几行就是余额最大的
                if (top.size() < TOP) {
                    top.add(new Partner(r.getPartnerName(), balance, overdue));
                }
            }
            if ((long) page * PAGE_SIZE >= result.total()) {
                break;
            }
        }
        BigDecimal previous = BigDecimal.ZERO;
        for (int page = 1; page <= PAGE_LIMIT; page++) {
            PageResult<AgingRow> result = billService.aging(page, PAGE_SIZE, kind, previousAsOf);
            previous = previous.add(Decimals.sum(result.records(), r -> Decimals.nz(r.getBalance())));
            if ((long) page * PAGE_SIZE >= result.total()) {
                break;
            }
        }
        return new Balance(asOf, now[0], now[1], now[2], AiReportMetrics.percent(now[1], now[0]), partners, previous,
                AiReportMetrics.growth(now[0], previous), top);
    }

    /**
     * 利润（利润表口径，已记账凭证，元）
     *
     * @param current          本月：营业收入 revenue、营业成本 cost、营业利润 operating、净利润 net
     * @param previousMonth    上月
     * @param sameMonthLastYear 去年同月
     * @param revenueChangePct 营业收入环比（%）
     * @param revenueYoyPct    营业收入同比（%）
     * @param netChangePct     净利润环比（%）
     * @param netMarginPct     本月净利率（%）
     */
    public record Profit(IncomeTrendRow current, IncomeTrendRow previousMonth, IncomeTrendRow sameMonthLastYear,
                         BigDecimal revenueChangePct, BigDecimal revenueYoyPct, BigDecimal netChangePct, BigDecimal netMarginPct) {
    }

    /**
     * 资金（现金、银行存款，元）
     *
     * @param opening          期初余额
     * @param inflow           本期收入
     * @param outflow          本期支出
     * @param closing          期末余额
     * @param netFlow          本期净流入
     * @param previousInflow   上期收入
     * @param previousOutflow  上期支出
     * @param inflowChangePct  收入环比（%）
     * @param outflowChangePct 支出环比（%）
     */
    public record Cash(BigDecimal opening, BigDecimal inflow, BigDecimal outflow, BigDecimal closing, BigDecimal netFlow,
                       BigDecimal previousInflow, BigDecimal previousOutflow, BigDecimal inflowChangePct, BigDecimal outflowChangePct) {
    }

    /**
     * 应收或应付余额（未核销，元）
     *
     * @param asOf              截至日期（期末）
     * @param overdue           已逾期
     * @param overdueOver90     逾期 90 天以上
     * @param overdueSharePct   逾期占余额的百分比
     * @param partners          有余额的往来单位数
     * @param previousBalance   上期末余额
     * @param balanceChangePct  余额比上期末的变化（%）
     * @param top               余额最大的 5 个往来单位
     */
    public record Balance(LocalDate asOf, BigDecimal balance, BigDecimal overdue, BigDecimal overdueOver90, BigDecimal overdueSharePct,
                          long partners, BigDecimal previousBalance, BigDecimal balanceChangePct, List<Partner> top) {
    }

    public record Partner(String name, BigDecimal balance, BigDecimal overdue) {
    }

    /**
     * 财务快照
     *
     * @param profit     利润（周报为空：利润表按月结账）
     * @param cash       资金
     * @param receivable 应收
     * @param payable    应付
     */
    public record FinanceSnapshot(Profit profit, Cash cash, Balance receivable, Balance payable) {
    }
}
