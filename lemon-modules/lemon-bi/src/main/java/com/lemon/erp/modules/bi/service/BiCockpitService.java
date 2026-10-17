package com.lemon.erp.modules.bi.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.bi.dto.BiInsight;
import com.lemon.erp.modules.bi.mapper.BiInsightMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.lemon.erp.common.util.Decimals.HUNDRED;

/**
 * 经营驾驶舱：销售、采购、利润的月度趋势和同比环比（数据仓库），应收应付、在途订单、生产、项目、客户服务等实时指标
 */
@Service
@RequiredArgsConstructor
public class BiCockpitService {

    private final BiInsightMapper insightMapper;
    private final BiEtlService etlService;

    public BiInsight.Cockpit cockpit() {
        BiDecisionService.requireAllData();
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth from = thisMonth.minusMonths(23);
        int fromPeriod = period(from);

        Map<Integer, BiInsight.PeriodRow> sales = byPeriod(insightMapper.selectSalesByPeriod(fromPeriod, null));
        Map<Integer, BiInsight.PeriodRow> purchase = byPeriod(insightMapper.selectPurchaseByPeriod(fromPeriod));
        Map<Integer, BiInsight.PeriodRow> finance = byPeriod(insightMapper.selectFinanceByPeriod(fromPeriod));

        // 近 12 个月趋势
        List<BiInsight.CockpitTrend> trend = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            int p = period(thisMonth.minusMonths(i));
            BigDecimal revenue = value(finance, p, BiInsight.PeriodRow::getRevenue);
            BigDecimal expense = value(finance, p, BiInsight.PeriodRow::getExpense);
            trend.add(new BiInsight.CockpitTrend(p, value(sales, p, BiInsight.PeriodRow::getAmount),
                    value(sales, p, BiInsight.PeriodRow::getProfit), value(purchase, p, BiInsight.PeriodRow::getAmount),
                    revenue, expense, revenue.subtract(expense)));
        }

        int cur = period(thisMonth);
        BigDecimal monthSales = value(sales, cur, BiInsight.PeriodRow::getAmount);
        BigDecimal lastMonthSales = value(sales, period(thisMonth.minusMonths(1)), BiInsight.PeriodRow::getAmount);
        BigDecimal lastYearSales = value(sales, period(thisMonth.minusYears(1)), BiInsight.PeriodRow::getAmount);
        BigDecimal yearSales = BigDecimal.ZERO;
        BigDecimal yearProfit = BigDecimal.ZERO;
        BigDecimal lastYtd = BigDecimal.ZERO;
        for (int m = 1; m <= thisMonth.getMonthValue(); m++) {
            yearSales = yearSales.add(value(sales, thisMonth.getYear() * 100 + m, BiInsight.PeriodRow::getAmount));
            yearProfit = yearProfit.add(value(sales, thisMonth.getYear() * 100 + m, BiInsight.PeriodRow::getProfit));
            lastYtd = lastYtd.add(value(sales, (thisMonth.getYear() - 1) * 100 + m, BiInsight.PeriodRow::getAmount));
        }
        // 实时指标直接汇总业务表，再补上数据仓库的销售同比环比
        BiInsight.CockpitKpi kpi = insightMapper.selectLive(today, LocalDateTime.now());
        if (kpi == null) {
            kpi = new BiInsight.CockpitKpi();
        }
        kpi.setMonthSales(monthSales);
        kpi.setMonthSalesMom(growth(monthSales, lastMonthSales));
        kpi.setMonthSalesYoy(growth(monthSales, lastYearSales));
        kpi.setMonthProfit(value(sales, cur, BiInsight.PeriodRow::getProfit));
        kpi.setMonthMargin(ratio(value(sales, cur, BiInsight.PeriodRow::getProfit), monthSales));
        kpi.setYearSales(yearSales);
        kpi.setYearSalesYoy(growth(yearSales, lastYtd));
        kpi.setYearMargin(ratio(yearProfit, yearSales));
        kpi.setMonthPurchase(value(purchase, cur, BiInsight.PeriodRow::getAmount));

        return new BiInsight.Cockpit(kpi, trend,
                insightMapper.selectTopCustomers(today.withDayOfYear(1), 5),
                insightMapper.selectTopMaterials(today.withDayOfYear(1), 5),
                insightMapper.selectInventoryByCategory(),
                etlService.lastSuccess());
    }

    static int period(YearMonth ym) {
        return ym.getYear() * 100 + ym.getMonthValue();
    }

    static Map<Integer, BiInsight.PeriodRow> byPeriod(List<BiInsight.PeriodRow> rows) {
        Map<Integer, BiInsight.PeriodRow> m = new HashMap<>();
        rows.forEach(r -> m.put(r.getPeriod(), r));
        return m;
    }

    static BigDecimal value(Map<Integer, BiInsight.PeriodRow> rows, int period, Function<BiInsight.PeriodRow, BigDecimal> field) {
        BiInsight.PeriodRow r = rows.get(period);
        return r == null || field.apply(r) == null ? BigDecimal.ZERO : field.apply(r);
    }

    /** 增长率（%），基数为 0 时为空 */
    static BigDecimal growth(BigDecimal cur, BigDecimal base) {
        return base.signum() == 0 ? null : cur.subtract(base).multiply(HUNDRED).divide(base.abs(), 1, RoundingMode.HALF_UP);
    }

    static BigDecimal ratio(BigDecimal num, BigDecimal den) {
        return Decimals.percent(num, den, 1);
    }
}
