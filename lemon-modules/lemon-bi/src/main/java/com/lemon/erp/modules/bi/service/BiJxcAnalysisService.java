package com.lemon.erp.modules.bi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.bi.dto.JxcQuery;
import com.lemon.erp.modules.bi.dto.JxcViews;
import com.lemon.erp.modules.bi.mapper.BiJxcMapper;
import com.lemon.erp.modules.bi.model.GoodsRankBy;
import com.lemon.erp.modules.bi.model.RfmSegment;
import com.lemon.erp.modules.fin.dto.CashierViews;
import com.lemon.erp.modules.fin.service.FinCashierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 进销存报表：资产状况（时点）、商品分析（畅销、滞销、积压、缺货、周转）、客户 RFM 分析
 */
@Service
@RequiredArgsConstructor
public class BiJxcAnalysisService {

    /** 往来余额排行的条数 */
    private static final int TOP_BALANCES = 10;
    /** 积压的默认可销天数阈值 */
    static final int DEFAULT_OVERSTOCK_DAYS = 90;
    /** 周转天数的计算精度 */
    private static final int RATIO_SCALE = 2;
    /** 资金账户中库存现金科目的编码前缀，其余为银行存款等 */
    private static final String CASH_CODE = "1001";

    private final BiJxcMapper jxcMapper;
    private final FinCashierService cashierService;

    // ---------------------------------------------------------------- 资产状况

    /**
     * 当前的资产状况：资金（现金、银行科目的余额，含未记账凭证）、库存金额（最新成本单价，没有核算时取参考成本价）、
     * 应收应付余额、预收预付余额。汇总全公司数据，需要「全部数据」的数据权限
     */
    public JxcViews.Assets assets() {
        BiDecisionService.requireAllData();
        LocalDate today = LocalDate.now();

        CashierViews.Daily daily = cashierService.daily(today, today, false);
        List<JxcViews.FundRow> funds = daily.rows().stream()
                .map(r -> new JxcViews.FundRow(r.accountId(), r.code(), r.name(), Decimals.money(r.close()))).toList();
        BigDecimal cash = Decimals.sum(funds.stream().filter(f -> f.code().startsWith(CASH_CODE)).toList(), JxcViews.FundRow::balance);
        BigDecimal bank = Decimals.sum(funds, JxcViews.FundRow::balance).subtract(cash);

        List<JxcViews.InventoryRow> inventory = jxcMapper.selectInventory();
        Map<String, JxcViews.NameAmount> byWarehouse = new LinkedHashMap<>();
        Map<String, JxcViews.NameAmount> byCategory = new LinkedHashMap<>();
        for (JxcViews.InventoryRow r : inventory) {
            BigDecimal amount = Decimals.money(r.getAmount());
            String wh = String.valueOf(r.getWarehouseId());
            byWarehouse.merge(wh, new JxcViews.NameAmount(wh, r.getWarehouseName(), amount),
                    (a, b) -> new JxcViews.NameAmount(a.key(), a.name(), a.amount().add(b.amount())));
            String cat = r.getCategory() == null ? "" : r.getCategory();
            byCategory.merge(cat, new JxcViews.NameAmount(cat, null, amount),
                    (a, b) -> new JxcViews.NameAmount(a.key(), null, a.amount().add(b.amount())));
        }
        Comparator<JxcViews.NameAmount> desc = Comparator.comparing(JxcViews.NameAmount::amount).reversed();
        List<JxcViews.NameAmount> warehouses = byWarehouse.values().stream().sorted(desc).toList();
        List<JxcViews.NameAmount> categories = byCategory.values().stream().sorted(desc).toList();
        BigDecimal inventoryAmount = Decimals.sum(warehouses, JxcViews.NameAmount::amount);

        JxcViews.Balances b = jxcMapper.selectBalances(today);
        if (b == null) {
            b = new JxcViews.Balances();
        }
        b.setArBalance(Decimals.money(b.getArBalance()));
        b.setArOverdue(Decimals.money(b.getArOverdue()));
        b.setApBalance(Decimals.money(b.getApBalance()));
        b.setAdvanceReceived(Decimals.money(b.getAdvanceReceived()));
        b.setAdvancePaid(Decimals.money(b.getAdvancePaid()));

        BigDecimal net = cash.add(bank).add(inventoryAmount).add(b.getArBalance()).add(b.getAdvancePaid())
                .subtract(b.getApBalance()).subtract(b.getAdvanceReceived());
        return new JxcViews.Assets(today, cash, bank, funds, inventoryAmount, warehouses, categories, b,
                jxcMapper.selectTopBalances("AR", TOP_BALANCES), jxcMapper.selectTopBalances("AP", TOP_BALANCES), net);
    }

    // ---------------------------------------------------------------- 商品分析

    /** 畅销：期间净销量大于 0，按净销量 / 净销售额 / 毛利排序 */
    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<JxcViews.GoodsRow> top(JxcQuery q, long current, long size) {
        goodsRange(q);
        if (q.getRankBy() == null) {
            q.setRankBy(GoodsRankBy.AMOUNT);
        }
        return page(jxcMapper.selectGoodsTop(Page.of(current, size), q), BiJxcAnalysisService::fillProfit);
    }

    /** 滞销：有库存、期间净销量不超过 slowQty（默认 0，即没有销售） */
    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<JxcViews.GoodsRow> slow(JxcQuery q, long current, long size) {
        goodsRange(q);
        if (q.getSlowQty() == null || q.getSlowQty().signum() < 0) {
            q.setSlowQty(BigDecimal.ZERO);
        }
        return page(jxcMapper.selectGoodsSlow(Page.of(current, size), q), r -> r.setStockAmount(Decimals.money(r.getStockAmount())));
    }

    /** 积压：可销天数 = 库存 / (期间净销量 / 期间天数) 超过 overstockDays（默认 90） */
    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<JxcViews.GoodsRow> overstock(JxcQuery q, long current, long size) {
        long days = goodsRange(q);
        if (q.getOverstockDays() == null || q.getOverstockDays() < 1) {
            q.setOverstockDays(DEFAULT_OVERSTOCK_DAYS);
        }
        return page(jxcMapper.selectGoodsOverstock(Page.of(current, size), q, days), r -> {
            r.setStockAmount(Decimals.money(r.getStockAmount()));
            r.setCoverDays(coverDays(r.getStockQty(), r.getSalesQty(), days));
        });
    }

    /** 缺货（当前）：可用库存低于安全库存，或不够未出库的销售订单 */
    public PageResult<JxcViews.GoodsRow> shortage(JxcQuery q, long current, long size) {
        return page(jxcMapper.selectGoodsShortage(Page.of(current, size), q), r -> r.setGapQty(Decimals.qty(
                Decimals.nz(r.getSafetyStock()).add(Decimals.nz(r.getSoOpen())).subtract(Decimals.nz(r.getAvailableQty())))));
    }

    /** 周转：周转次数 = 净销量 / 平均库存，周转天数 = 期间天数 / 周转次数（按数量） */
    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<JxcViews.GoodsRow> turnover(JxcQuery q, long current, long size) {
        long days = goodsRange(q);
        return page(jxcMapper.selectGoodsTurnover(Page.of(current, size), q), r -> {
            fillProfit(r);
            BigDecimal[] t = turnover(r.getOpenQty(), r.getCloseQty(), r.getSalesQty(), days);
            r.setTurnover(t[0]);
            r.setTurnoverDays(t[1]);
        });
    }

    // ---------------------------------------------------------------- RFM

    /** 客户 RFM：期间有销售出库的客户，按最近购买、频次、净销售额打分分群 */
    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public JxcViews.Rfm rfm(JxcQuery q) {
        BiJxcService.checkRange(q, BiJxcService.MAX_DAYS_LONG);
        LocalDate asOf = q.getEnd().isBefore(LocalDate.now()) ? q.getEnd() : LocalDate.now();
        List<JxcViews.RfmRow> rows = new ArrayList<>(jxcMapper.selectRfm(q));
        for (JxcViews.RfmRow r : rows) {
            r.setMonetary(Decimals.money(r.getMonetary()));
            r.setRecency(r.getLastDate() == null ? null : Math.max(0, ChronoUnit.DAYS.between(r.getLastDate(), asOf)));
        }
        BigDecimal[] avg = RfmScorer.score(rows);
        rows.sort(Comparator.comparing((JxcViews.RfmRow r) -> r.getR() + r.getF() + r.getM()).reversed()
                .thenComparing(JxcViews.RfmRow::getMonetary, Comparator.reverseOrder()));
        Map<RfmSegment, int[]> counts = new LinkedHashMap<>();
        Map<RfmSegment, BigDecimal> amounts = new LinkedHashMap<>();
        for (RfmSegment s : RfmSegment.values()) {
            counts.put(s, new int[1]);
            amounts.put(s, BigDecimal.ZERO);
        }
        for (JxcViews.RfmRow r : rows) {
            counts.get(r.getSegment())[0]++;
            amounts.merge(r.getSegment(), r.getMonetary(), BigDecimal::add);
        }
        List<JxcViews.SegmentStat> segments = new ArrayList<>();
        counts.forEach((s, c) -> segments.add(new JxcViews.SegmentStat(s, c[0], amounts.get(s))));
        return new JxcViews.Rfm(asOf, rows, segments, avg[0], avg[1], avg[2]);
    }

    // ---------------------------------------------------------------- 计算

    /** 可销天数 = 库存 × 期间天数 / 期间净销量；没有销量时为空 */
    static BigDecimal coverDays(BigDecimal stock, BigDecimal sales, long days) {
        if (sales == null || sales.signum() <= 0) {
            return null;
        }
        return Decimals.nz(stock).multiply(BigDecimal.valueOf(days)).divide(sales, 1, RoundingMode.HALF_UP);
    }

    /**
     * 周转次数 = 净销量 / ((期初 + 期末) / 2)，平均库存不大于 0 时为空；周转天数 = 期间天数 / 周转次数，周转次数为 0 时为空
     *
     * @return [周转次数, 周转天数]
     */
    static BigDecimal[] turnover(BigDecimal open, BigDecimal close, BigDecimal sales, long days) {
        BigDecimal avg = Decimals.nz(open).add(Decimals.nz(close)).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
        if (avg.signum() <= 0) {
            return new BigDecimal[]{null, null};
        }
        BigDecimal times = Decimals.nz(sales).max(BigDecimal.ZERO).divide(avg, RATIO_SCALE, RoundingMode.HALF_UP);
        BigDecimal turnoverDays = times.signum() == 0 ? null
                : BigDecimal.valueOf(days).divide(times, 1, RoundingMode.HALF_UP);
        return new BigDecimal[]{times, turnoverDays};
    }

    private static void fillProfit(JxcViews.GoodsRow r) {
        r.setSalesAmount(Decimals.money(r.getSalesAmount()));
        r.setCost(Decimals.money(r.getCost()));
        r.setProfit(r.getSalesAmount().subtract(r.getCost()));
        r.setMargin(Decimals.percent(r.getProfit(), r.getSalesAmount(), 2));
    }

    /** 校验日期范围，返回期间天数 */
    private static long goodsRange(JxcQuery q) {
        BiJxcService.checkRange(q, BiJxcService.MAX_DAYS);
        return ChronoUnit.DAYS.between(q.getBegin(), q.getEnd()) + 1;
    }

    private static PageResult<JxcViews.GoodsRow> page(Page<JxcViews.GoodsRow> page, Consumer<JxcViews.GoodsRow> fill) {
        page.getRecords().forEach(fill);
        return PageResult.of(page);
    }
}
