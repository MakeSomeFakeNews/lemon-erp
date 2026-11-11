package com.lemon.erp.modules.bi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.bi.dto.JxcQuery;
import com.lemon.erp.modules.bi.dto.JxcViews;
import com.lemon.erp.modules.bi.mapper.BiJxcMapper;
import com.lemon.erp.modules.bi.model.JxcDimension;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.DateTimeException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 进销存报表：销售报表、进货报表（汇总 → 商品明细，或汇总 → 单据列表 → 单据明细）、经营状况。
 * 实时汇总 scm_trade_doc（不依赖数据仓库），按出入库单的制单人部门 + 制单人做数据权限；净额 = 出库（入库）− 退货
 */
@Service
@RequiredArgsConstructor
public class BiJxcService {

    /** 汇总最多返回的行数（按净额从大到小），合计仍为全部 */
    static final int SUMMARY_LIMIT = 1000;
    /** 明细导出最多行数 */
    static final int EXPORT_LIMIT = 5000;
    /** 销售、进货、商品分析的最大日期跨度（天） */
    static final int MAX_DAYS = 366;
    /** 经营状况（按月走势）、RFM 的最大日期跨度（天） */
    static final int MAX_DAYS_LONG = 731;
    /** 客户销售分布单独列出的客户数 */
    private static final int TOP_CUSTOMERS = 10;

    private final BiJxcMapper jxcMapper;

    // ---------------------------------------------------------------- 销售 / 进货

    /** 汇总；sales = true 销售报表，false 进货报表 */
    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public JxcViews.Summary summary(boolean sales, JxcDimension dim, JxcQuery q) {
        prepare(sales, q, MAX_DAYS);
        if (dim == null || !dim.supports(sales)) {
            throw new BizException("bi.error.jxcInvalidDimension", String.valueOf(dim));
        }
        return doSummary(sales, dim, q);
    }

    /** 明细（分页），带汇总的筛选条件和穿透的维度取值 */
    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<JxcViews.DetailRow> detail(boolean sales, JxcQuery q, long current, long size) {
        prepare(sales, q, MAX_DAYS);
        Page<JxcViews.DetailRow> page = jxcMapper.selectDetail(Page.of(current, size), sales, q);
        page.getRecords().forEach(r -> fillDetail(sales, r));
        return PageResult.of(page);
    }

    /** 导出明细：最多 {@link #EXPORT_LIMIT} 行 */
    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public JxcViews.DetailExport export(boolean sales, JxcQuery q) {
        prepare(sales, q, MAX_DAYS);
        List<JxcViews.DetailRow> list = jxcMapper.selectDetailList(sales, q, EXPORT_LIMIT + 1);
        List<JxcViews.DetailRow> rows = new ArrayList<>(list.subList(0, Math.min(list.size(), EXPORT_LIMIT)));
        rows.forEach(r -> fillDetail(sales, r));
        return new JxcViews.DetailExport(rows, list.size() > EXPORT_LIMIT);
    }

    /**
     * 单据列表（分页）：汇总项下的出入库单 / 退货单，每张一行，金额为该单中符合条件的明细合计（与明细同一取数和口径），
     * summary 为全部单据的合计（与汇总行一致）
     */
    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public JxcViews.DocPage docs(boolean sales, JxcQuery q, long current, long size) {
        prepare(sales, q, MAX_DAYS);
        Page<JxcViews.DocRow> page = jxcMapper.selectDocs(Page.of(current, size), sales, q);
        page.getRecords().forEach(r -> fillDoc(sales, r));
        JxcViews.SummaryRow total = jxcMapper.selectTotal(sales, q);
        if (total == null) {
            total = new JxcViews.SummaryRow();
        }
        fillSummary(sales, total);
        return new JxcViews.DocPage(page.getRecords(), page.getTotal(), total);
    }

    /** 导出单据列表：最多 {@link #EXPORT_LIMIT} 行 */
    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public JxcViews.DocExport exportDocs(boolean sales, JxcQuery q) {
        prepare(sales, q, MAX_DAYS);
        List<JxcViews.DocRow> list = jxcMapper.selectDocList(sales, q, EXPORT_LIMIT + 1);
        List<JxcViews.DocRow> rows = new ArrayList<>(list.subList(0, Math.min(list.size(), EXPORT_LIMIT)));
        rows.forEach(r -> fillDoc(sales, r));
        return new JxcViews.DocExport(rows, list.size() > EXPORT_LIMIT);
    }

    // ---------------------------------------------------------------- 经营状况

    /**
     * 经营状况：销售及利润走势（按月）、资金流走势（收付款单 + 预收预付款 + 票据托收、贴现、兑付，收票、背书、开票不计入，需要「全部数据」权限）、员工销售情况、客户销售分布
     */
    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public JxcViews.Business business(JxcQuery q) {
        prepare(true, q, MAX_DAYS_LONG);
        List<YearMonth> months = months(q.getBegin(), q.getEnd());

        JxcViews.Summary byMonth = doSummary(true, JxcDimension.MONTH, q);
        Map<String, JxcViews.SummaryRow> monthRows = new HashMap<>();
        byMonth.rows().forEach(r -> monthRows.put(r.getGroupKey(), r));
        List<JxcViews.SummaryRow> trend = new ArrayList<>();
        for (YearMonth ym : months) {
            String key = String.valueOf(period(ym));
            JxcViews.SummaryRow r = monthRows.get(key);
            if (r == null) {
                r = new JxcViews.SummaryRow();
                r.setGroupKey(key);
                fillSummary(true, r);
            }
            trend.add(r);
        }

        List<JxcViews.CashRow> cash = null;
        if (SecurityUtils.getDataScope().all()) {
            Map<Integer, JxcViews.CashRow> byPeriod = new HashMap<>();
            for (YearMonth ym : months) {
                JxcViews.CashRow c = new JxcViews.CashRow();
                c.setPeriod(period(ym));
                c.setReceipt(BigDecimal.ZERO);
                c.setPayment(BigDecimal.ZERO);
                byPeriod.put(c.getPeriod(), c);
            }
            List<JxcViews.CashRow> source = new ArrayList<>(jxcMapper.selectSettlementByMonth(q.getBegin(), q.getEnd()));
            source.addAll(jxcMapper.selectAdvanceByMonth(q.getBegin(), q.getEnd()));
            source.addAll(jxcMapper.selectNoteCashByMonth(q.getBegin(), q.getEnd()));
            for (JxcViews.CashRow r : source) {
                JxcViews.CashRow c = byPeriod.get(r.getPeriod());
                if (c != null) {
                    c.setReceipt(c.getReceipt().add(Decimals.nz(r.getReceipt())));
                    c.setPayment(c.getPayment().add(Decimals.nz(r.getPayment())));
                }
            }
            cash = new ArrayList<>();
            for (YearMonth ym : months) {
                JxcViews.CashRow c = byPeriod.get(period(ym));
                c.setNet(c.getReceipt().subtract(c.getPayment()));
                cash.add(c);
            }
        }

        JxcViews.Summary salesmen = doSummary(true, JxcDimension.SALESMAN, q);
        JxcViews.Summary customers = doSummary(true, JxcDimension.CUSTOMER, q);
        List<JxcViews.SummaryRow> top = new ArrayList<>(customers.rows().subList(0, Math.min(TOP_CUSTOMERS, customers.rows().size())));
        if (customers.rows().size() > TOP_CUSTOMERS) {
            JxcViews.SummaryRow others = new JxcViews.SummaryRow();
            BigDecimal topAmount = Decimals.sum(top, JxcViews.SummaryRow::getNetAmount);
            BigDecimal topProfit = Decimals.sum(top, JxcViews.SummaryRow::getProfit);
            others.setNetAmount(customers.total().getNetAmount().subtract(topAmount));
            others.setProfit(customers.total().getProfit().subtract(topProfit));
            others.setMargin(Decimals.percent(others.getProfit(), others.getNetAmount(), 2));
            top.add(others);
        }
        return new JxcViews.Business(byMonth.total(), trend, cash, salesmen.rows(), top, customers.rows().size());
    }

    // ---------------------------------------------------------------- 内部

    private JxcViews.Summary doSummary(boolean sales, JxcDimension dim, JxcQuery q) {
        List<JxcViews.SummaryRow> list = jxcMapper.selectSummary(sales, dim.name(), q, SUMMARY_LIMIT + 1);
        List<JxcViews.SummaryRow> rows = new ArrayList<>(list.subList(0, Math.min(list.size(), SUMMARY_LIMIT)));
        rows.forEach(r -> {
            if (r.getGroupKey() != null && r.getGroupKey().isEmpty()) {
                r.setGroupKey(null);
            }
            fillSummary(sales, r);
        });
        JxcViews.SummaryRow total = jxcMapper.selectTotal(sales, q);
        if (total == null) {
            total = new JxcViews.SummaryRow();
        }
        fillSummary(sales, total);
        return new JxcViews.Summary(rows, total, list.size() > SUMMARY_LIMIT);
    }

    /** 统一精度，计算毛利、毛利率、平均单价 */
    static void fillSummary(boolean sales, JxcViews.SummaryRow r) {
        r.setBizQty(Decimals.qty(r.getBizQty()));
        r.setReturnQty(Decimals.qty(r.getReturnQty()));
        r.setNetQty(Decimals.qty(r.getNetQty()));
        r.setBizAmount(Decimals.money(r.getBizAmount()));
        r.setReturnAmount(Decimals.money(r.getReturnAmount()));
        r.setNetAmount(Decimals.money(r.getNetAmount()));
        r.setTaxedAmount(Decimals.money(r.getTaxedAmount()));
        r.setDocCount(r.getDocCount() == null ? 0L : r.getDocCount());
        r.setMissingCost(r.getMissingCost() == null ? 0L : r.getMissingCost());
        r.setAvgPrice(r.getNetQty().signum() == 0 ? null
                : r.getNetAmount().divide(r.getNetQty(), Decimals.PRICE_SCALE, RoundingMode.HALF_UP));
        if (sales) {
            r.setCost(Decimals.money(r.getCost()));
            r.setProfit(r.getNetAmount().subtract(r.getCost()));
            r.setMargin(Decimals.percent(r.getProfit(), r.getNetAmount(), 2));
            r.setBadDebt(Decimals.money(r.getBadDebt()));
            r.setFreight(Decimals.money(r.getFreight()));
            r.setProfitAfterBadDebt(r.getProfit().subtract(r.getBadDebt()));
            r.setProfitAfterFreight(r.getProfitAfterBadDebt().subtract(r.getFreight()));
        } else {
            r.setCost(null);
            r.setMissingCost(null);
            r.setBadDebt(null);
            r.setFreight(null);
        }
    }

    private static void fillDetail(boolean sales, JxcViews.DetailRow r) {
        r.setAmount(Decimals.money(r.getAmount()));
        r.setTaxedAmount(Decimals.money(r.getTaxedAmount()));
        if (sales) {
            r.setCost(Decimals.money(r.getCost()));
            r.setProfit(r.getAmount().subtract(r.getCost()));
            r.setBadDebt(Decimals.money(r.getBadDebt()));
            r.setFreight(Decimals.money(r.getFreight()));
            r.setProfitAfterBadDebt(r.getProfit().subtract(r.getBadDebt()));
            r.setProfitAfterFreight(r.getProfitAfterBadDebt().subtract(r.getFreight()));
        }
    }

    static void fillDoc(boolean sales, JxcViews.DocRow r) {
        r.setQty(Decimals.qty(r.getQty()));
        r.setAmount(Decimals.money(r.getAmount()));
        r.setTaxedAmount(Decimals.money(r.getTaxedAmount()));
        r.setDiscount(Decimals.money(r.getDiscount()));
        if (sales) {
            r.setCost(Decimals.money(r.getCost()));
            r.setProfit(r.getAmount().subtract(r.getCost()));
            r.setMargin(Decimals.percent(r.getProfit(), r.getAmount(), 2));
            r.setBadDebt(Decimals.money(r.getBadDebt()));
            r.setFreight(Decimals.money(r.getFreight()));
            r.setProfitAfterBadDebt(r.getProfit().subtract(r.getBadDebt()));
            r.setProfitAfterFreight(r.getProfitAfterBadDebt().subtract(r.getFreight()));
            r.setMissingCost(r.getMissingCost() == null ? 0L : r.getMissingCost());
        } else {
            r.setMissingCost(null);
        }
    }

    /** 校验日期范围，把穿透的维度取值转换为筛选条件 */
    static void prepare(boolean sales, JxcQuery q, int maxDays) {
        checkRange(q, maxDays);
        if (!sales) {
            q.setSalesmanId(null);
            q.setRegion(null);
        }
        JxcDimension dim = q.getDim();
        if (dim == null) {
            return;
        }
        if (!dim.supports(sales)) {
            throw new BizException("bi.error.jxcInvalidDimension", dim.name());
        }
        String key = q.getKey();
        if (!StringUtils.hasText(key)) {
            if (dim == JxcDimension.CATEGORY || dim == JxcDimension.BRAND || dim == JxcDimension.REGION || dim == JxcDimension.SALESMAN) {
                q.markNone(dim);
                return;
            }
            throw new BizException("bi.error.jxcInvalidKey", "");
        }
        try {
            switch (dim) {
                case CATEGORY -> q.drillCategory(key);
                case BRAND -> q.setBrand(key);
                case REGION -> q.setRegion(key);
                case CUSTOMER, SUPPLIER -> q.setPartnerId(Long.valueOf(key));
                case SALESMAN -> q.setSalesmanId(Long.valueOf(key));
                case MATERIAL -> q.setMaterialId(Long.valueOf(key));
                case DAY -> narrow(q, LocalDate.parse(key), LocalDate.parse(key));
                case MONTH -> {
                    int p = Integer.parseInt(key);
                    YearMonth ym = YearMonth.of(p / 100, p % 100);
                    narrow(q, ym.atDay(1), ym.atEndOfMonth());
                }
                default -> {
                }
            }
        } catch (NumberFormatException | DateTimeException e) {
            throw new BizException("bi.error.jxcInvalidKey", key);
        }
    }

    static void checkRange(JxcQuery q, int maxDays) {
        if (q.getBegin() == null || q.getEnd() == null) {
            throw new BizException("bi.error.jxcDateRangeRequired");
        }
        if (q.getEnd().isBefore(q.getBegin())) {
            throw new BizException("bi.error.jxcEndBeforeBegin");
        }
        if (ChronoUnit.DAYS.between(q.getBegin(), q.getEnd()) + 1 > maxDays) {
            throw new BizException("bi.error.jxcDateRangeTooLong", String.valueOf(maxDays));
        }
    }

    /** 穿透到某天 / 某月：与原日期范围取交集（没有交集时结果为空） */
    private static void narrow(JxcQuery q, LocalDate begin, LocalDate end) {
        q.setBegin(begin.isAfter(q.getBegin()) ? begin : q.getBegin());
        q.setEnd(end.isBefore(q.getEnd()) ? end : q.getEnd());
        if (q.getEnd().isBefore(q.getBegin())) {
            q.setEnd(q.getBegin().minusDays(1));
        }
    }

    static List<YearMonth> months(LocalDate begin, LocalDate end) {
        List<YearMonth> list = new ArrayList<>();
        for (YearMonth ym = YearMonth.from(begin); !ym.isAfter(YearMonth.from(end)); ym = ym.plusMonths(1)) {
            list.add(ym);
        }
        return list;
    }

    static int period(YearMonth ym) {
        return ym.getYear() * 100 + ym.getMonthValue();
    }
}
