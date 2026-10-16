package com.lemon.erp.modules.bi.service;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 经营驾驶舱：销售、采购、利润的月度趋势和同比环比（数据仓库），应收应付、在途订单、生产、项目、客户服务等实时指标
 */
@Service
@RequiredArgsConstructor
public class BiCockpitService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BiInsightMapper insightMapper;
    private final BiEtlService etlService;

    public Map<String, Object> cockpit() {
        BiDecisionService.requireAllData();
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth from = thisMonth.minusMonths(23);
        int fromPeriod = period(from);

        Map<Integer, Map<String, Object>> sales = byPeriod(insightMapper.selectSalesByPeriod(fromPeriod, null));
        Map<Integer, Map<String, Object>> purchase = byPeriod(insightMapper.selectPurchaseByPeriod(fromPeriod));
        Map<Integer, Map<String, Object>> finance = byPeriod(insightMapper.selectFinanceByPeriod(fromPeriod));

        // 近 12 个月趋势
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            int p = period(thisMonth.minusMonths(i));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("period", p);
            row.put("sales", value(sales, p, "amount"));
            row.put("profit", value(sales, p, "profit"));
            row.put("purchase", value(purchase, p, "amount"));
            BigDecimal revenue = value(finance, p, "revenue");
            BigDecimal expense = value(finance, p, "expense");
            row.put("revenue", revenue);
            row.put("expense", expense);
            row.put("netProfit", revenue.subtract(expense));
            trend.add(row);
        }

        int cur = period(thisMonth);
        BigDecimal monthSales = value(sales, cur, "amount");
        BigDecimal lastMonthSales = value(sales, period(thisMonth.minusMonths(1)), "amount");
        BigDecimal lastYearSales = value(sales, period(thisMonth.minusYears(1)), "amount");
        BigDecimal yearSales = BigDecimal.ZERO;
        BigDecimal yearProfit = BigDecimal.ZERO;
        BigDecimal lastYtd = BigDecimal.ZERO;
        for (int m = 1; m <= thisMonth.getMonthValue(); m++) {
            yearSales = yearSales.add(value(sales, thisMonth.getYear() * 100 + m, "amount"));
            yearProfit = yearProfit.add(value(sales, thisMonth.getYear() * 100 + m, "profit"));
            lastYtd = lastYtd.add(value(sales, (thisMonth.getYear() - 1) * 100 + m, "amount"));
        }
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("monthSales", monthSales);
        kpi.put("monthSalesMom", growth(monthSales, lastMonthSales));
        kpi.put("monthSalesYoy", growth(monthSales, lastYearSales));
        kpi.put("monthProfit", value(sales, cur, "profit"));
        kpi.put("monthMargin", ratio(value(sales, cur, "profit"), monthSales));
        kpi.put("yearSales", yearSales);
        kpi.put("yearSalesYoy", growth(yearSales, lastYtd));
        kpi.put("yearMargin", ratio(yearProfit, yearSales));
        kpi.put("monthPurchase", value(purchase, cur, "amount"));
        kpi.putAll(insightMapper.selectLive(today, LocalDateTime.now()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kpi", kpi);
        result.put("trend", trend);
        result.put("topCustomers", insightMapper.selectTopCustomers(today.withDayOfYear(1), 5));
        result.put("topMaterials", insightMapper.selectTopMaterials(today.withDayOfYear(1), 5));
        result.put("inventory", insightMapper.selectInventoryByCategory());
        result.put("lastSync", etlService.lastSuccess());
        return result;
    }

    static int period(YearMonth ym) {
        return ym.getYear() * 100 + ym.getMonthValue();
    }

    static Map<Integer, Map<String, Object>> byPeriod(List<Map<String, Object>> rows) {
        Map<Integer, Map<String, Object>> m = new HashMap<>();
        rows.forEach(r -> m.put(((Number) r.get("period")).intValue(), r));
        return m;
    }

    static BigDecimal value(Map<Integer, Map<String, Object>> rows, int period, String key) {
        Map<String, Object> r = rows.get(period);
        return r == null || r.get(key) == null ? BigDecimal.ZERO : new BigDecimal(r.get(key).toString());
    }

    /** 增长率（%），基数为 0 时为空 */
    static BigDecimal growth(BigDecimal cur, BigDecimal base) {
        return base.signum() == 0 ? null : cur.subtract(base).multiply(HUNDRED).divide(base.abs(), 1, RoundingMode.HALF_UP);
    }

    static BigDecimal ratio(BigDecimal num, BigDecimal den) {
        return den.signum() == 0 ? null : num.multiply(HUNDRED).divide(den, 1, RoundingMode.HALF_UP);
    }
}
