package com.lemon.erp.modules.bi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.Sort;
import com.lemon.erp.framework.data.SortParam;
import com.lemon.erp.framework.data.Sorts;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.bi.dto.JxcViews;
import com.lemon.erp.modules.bi.dto.StockAnalysisQuery;
import com.lemon.erp.modules.bi.mapper.BiJxcMapper;
import com.lemon.erp.modules.bi.model.SalesRateBase;
import com.lemon.erp.modules.bi.model.SalesRateMode;
import com.lemon.erp.modules.bi.model.StockAlertTab;
import com.lemon.erp.modules.bi.model.TurnoverBasis;
import com.lemon.erp.modules.scm.service.ScmPricingService;
import com.lemon.erp.modules.wms.model.PriceLevels;
import com.lemon.erp.modules.wms.service.WmsPriceLevelService;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Set;

/**
 * 库存分析（进销存报表，issue #116）：周转分析、滞销 / 畅销分析、库存预警。商品条件、截至日库存的推算、销量口径
 * （销售出库 − 销售退货，按所选仓库）与积压分析相同，统计在 SQL 中完成（BiJxcMapper.xml「库存分析」）。
 * 销量、进货量按出入库单的制单人部门 + 制单人做数据权限（与商品分析、积压分析一致）。
 * 金额按权限过滤：库存均价、库存金额、资金占用需要「查看成本金额」，参考进价需要采购查看权限，
 * 零售价、批发价需要售价查看权限或「商品价格」页面权限（规则同商品速查），停用的价格列不返回。
 * 列头排序（issue #122）按各报表的白名单（sortFields），没有权限、不返回的列不能排序
 */
@Service
@RequiredArgsConstructor
public class BiStockAnalysisService {

    /** 销售比例阈值的默认值（%）：滞销不高于 80，畅销不低于 10 */
    static final BigDecimal SLOW_RATIO = BigDecimal.valueOf(80);
    static final BigDecimal HOT_RATIO = BigDecimal.TEN;
    /** 「商品价格」页面权限：可以查看零售价、批发价 */
    static final String GOODS_PRICE_PERM = "scm:goodsPrice:list";
    /** 周转率、销售比例、系数的小数位 */
    private static final int RATE_SCALE = 2;
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    /** 商品列（各报表共有） */
    private static final String[] GOODS_SORTS = {"code", "name", "category", "brand", "spec", "unit"};

    private final BiJxcMapper jxcMapper;
    private final PermissionService permissionService;
    private final ScmPricingService pricingService;
    private final WmsPriceLevelService priceLevelService;

    // ---------------------------------------------------------------- 周转分析

    /**
     * 周转分析：库存周转天数 = 期间天数 × (期初 + 期末) ÷ 2 ÷ 本期出库，库存周转率 = 本期出库 ÷ ((期初 + 期末) ÷ 2) × 100%。
     * 按金额维度时用 数量 × 单价 计算（单价为空的商品不计算），合计行按合计的数量或金额计算
     */
    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public JxcViews.TurnoverPage turnover(StockAnalysisQuery q, SortParam sort, long current, long size) {
        long days = BiJxcService.checkRange(q.getBegin(), q.getEnd(), BiJxcService.MAX_DAYS);
        if (q.getBasis() == null) {
            q.setBasis(TurnoverBasis.QTY);
        }
        if (q.getExcludeIdle() == null) {
            q.setExcludeIdle(true);
        }
        boolean cost = permissionService.hasPerm(WmsStockDocService.COST_PERM);
        checkBasis(q, cost);
        q.resolve(null, cost);
        boolean amountVisible = q.getBasis() != TurnoverBasis.QTY || cost;
        Sort order = turnoverSorts(amountVisible).resolve(sort);

        Page<JxcViews.TurnoverRow> page = jxcMapper.selectTurnover(Page.of(current, size), q, order);
        for (JxcViews.TurnoverRow r : page.getRecords()) {
            BigDecimal price = r.getPrice();
            r.setOutAmount(amount(r.getOutQty(), price));
            r.setOpenAmount(amount(r.getOpenQty(), price));
            r.setCloseAmount(amount(r.getCloseQty(), price));
            fillTurnover(r, q.getBasis(), days, amountVisible);
        }
        JxcViews.TurnoverRow summary = jxcMapper.selectTurnoverTotal(q);
        if (summary == null) {
            summary = new JxcViews.TurnoverRow();
        }
        summary.setOutAmount(summary.getOutAmount() == null ? null : Decimals.money(summary.getOutAmount()));
        summary.setOpenAmount(summary.getOpenAmount() == null ? null : Decimals.money(summary.getOpenAmount()));
        summary.setCloseAmount(summary.getCloseAmount() == null ? null : Decimals.money(summary.getCloseAmount()));
        fillTurnover(summary, q.getBasis(), days, amountVisible);
        return new JxcViews.TurnoverPage(page.getRecords(), page.getTotal(), summary, days, q.getBasis(), amountVisible);
    }

    /** 周转分析可排序的列：金额列只在返回时（按金额维度，或按数量且有「查看成本金额」） */
    public static Sorts.Fields turnoverSorts(boolean amountVisible) {
        return Sorts.fields(GOODS_SORTS).add("turnoverDays", "turnoverRate", "outQty", "openQty", "closeQty")
                .addIf(amountVisible, "outAmount", "openAmount", "closeAmount");
    }

    /** 金额维度的权限与价格列：成本价需要「查看成本金额」，零售价 / 批发价需要售价查看权限且价格列已启用 */
    private void checkBasis(StockAnalysisQuery q, boolean cost) {
        switch (q.getBasis()) {
            case COST -> {
                if (!cost) {
                    throw new BizException("bi.error.turnoverCostDenied");
                }
            }
            case RETAIL, WHOLESALE -> {
                if (!salesVisible()) {
                    throw new BizException("bi.error.turnoverPriceDenied");
                }
                int level = q.getBasis() == TurnoverBasis.RETAIL ? PriceLevels.RETAIL
                        : q.getPriceLevel() == null ? -1 : q.getPriceLevel();
                if (level < PriceLevels.RETAIL || level > PriceLevels.MAX || (q.getBasis() == TurnoverBasis.WHOLESALE && level == PriceLevels.RETAIL)
                        || !priceLevelService.isEnabled(level)) {
                    throw new BizException("bi.error.turnoverPriceLevel");
                }
                q.setPriceLevel(level);
            }
            default -> {
            }
        }
    }

    private static BigDecimal amount(BigDecimal qty, BigDecimal price) {
        return price == null ? null : Decimals.money(Decimals.nz(qty).multiply(price));
    }

    /** 周转天数、周转率；按数量时用数量，按金额时用金额；不返回金额时清空金额列 */
    private static void fillTurnover(JxcViews.TurnoverRow r, TurnoverBasis basis, long days, boolean amountVisible) {
        BigDecimal[] t = basis == TurnoverBasis.QTY
                ? turnover(r.getOpenQty(), r.getCloseQty(), r.getOutQty(), days)
                : r.getOutAmount() == null ? new BigDecimal[]{null, null}
                : turnover(r.getOpenAmount(), r.getCloseAmount(), r.getOutAmount(), days);
        r.setTurnoverDays(t[0]);
        r.setTurnoverRate(t[1]);
        if (!amountVisible) {
            r.setOutAmount(null);
            r.setOpenAmount(null);
            r.setCloseAmount(null);
        }
    }

    /**
     * 平均库存 = (期初 + 期末) ÷ 2，不大于 0 时都为空；周转天数 = 期间天数 × 平均库存 ÷ 出库，出库不大于 0 时为空；
     * 周转率（%）= 出库 ÷ 平均库存 × 100，出库为负（退货多于出库）时按 0
     *
     * @return [周转天数, 周转率]
     */
    static BigDecimal[] turnover(BigDecimal open, BigDecimal close, BigDecimal out, long days) {
        BigDecimal avg = Decimals.nz(open).add(Decimals.nz(close)).divide(TWO, 6, RoundingMode.HALF_UP);
        if (avg.signum() <= 0) {
            return new BigDecimal[]{null, null};
        }
        BigDecimal o = Decimals.nz(out).max(BigDecimal.ZERO);
        BigDecimal turnoverDays = o.signum() == 0 ? null
                : avg.multiply(BigDecimal.valueOf(days)).divide(o, 1, RoundingMode.HALF_UP);
        return new BigDecimal[]{turnoverDays, Decimals.percent(o, avg, RATE_SCALE)};
    }

    // ---------------------------------------------------------------- 滞销 / 畅销分析

    /**
     * 滞销 / 畅销：销售比例 = 期间净销量 ÷ 销售基数 × 100%，滞销列出不高于阈值的、畅销列出不低于阈值的；
     * 基数不大于 0 的商品（期间没有进货，或期初加进货不大于 0）无法计算比例，不参与
     */
    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public JxcViews.SalesRatePage salesRate(StockAnalysisQuery q, SortParam sort, long current, long size) {
        BiJxcService.checkRange(q.getBegin(), q.getEnd(), BiJxcService.MAX_DAYS);
        if (q.getMode() == null) {
            q.setMode(SalesRateMode.SLOW);
        }
        if (q.getBase() == null) {
            q.setBase(SalesRateBase.PURCHASE);
        }
        if (q.getRatio() == null) {
            q.setRatio(q.getMode() == SalesRateMode.HOT ? HOT_RATIO : SLOW_RATIO);
        } else if (q.getRatio().signum() < 0) {
            throw new BizException("bi.error.salesRateRatio");
        }
        Sort order = salesRateSorts(q.getBase()).resolve(sort);
        Page<JxcViews.SalesRateRow> page = jxcMapper.selectSalesRate(Page.of(current, size), q, order);
        page.getRecords().forEach(r -> r.setRatio(Decimals.percent(r.getSalesQty(), r.getBaseQty(), RATE_SCALE)));
        JxcViews.SalesRateRow summary = jxcMapper.selectSalesRateTotal(q);
        if (summary == null) {
            summary = new JxcViews.SalesRateRow();
        }
        summary.setRatio(Decimals.percent(summary.getSalesQty(), summary.getBaseQty(), RATE_SCALE));
        return new JxcViews.SalesRatePage(page.getRecords(), page.getTotal(), summary, q.getMode(), q.getBase(), q.getRatio());
    }

    /** 滞销 / 畅销可排序的列：期初库存、本期进货只在基数为「期初库存 + 本期进货量」时显示 */
    public static Sorts.Fields salesRateSorts(SalesRateBase base) {
        return Sorts.fields(GOODS_SORTS).add("salesQty", "baseQty", "ratio")
                .addIf(base == SalesRateBase.OPENING_PURCHASE, "openQty", "purchaseQty");
    }

    // ---------------------------------------------------------------- 库存预警

    /**
     * 库存预警：截至某日（默认今天，不能晚于今天）的库存与上下限。「仓库」每个仓库 + 商品一行（明细与预警状态），
     * 「高于上限」积压数量 = 库存 − 上限、资金占用 = 库存均价 × 积压数量，「低于下限」补货数量 = 下限 − 库存、
     * 补货资金 = 库存均价 × 补货数量；系数 = 数量 ÷ 基数。预警口径同库存查询（按总库存比较所有仓库的合计，分仓库按仓库）
     */
    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public JxcViews.StockAlertPage stockAlert(StockAnalysisQuery q, SortParam sort, long current, long size) {
        LocalDate today = LocalDate.now();
        if (q.getDate() == null) {
            q.setDate(today);
        } else if (q.getDate().isAfter(today)) {
            throw new BizException("bi.error.overstockDateAfterToday");
        }
        if (q.getTab() == null) {
            q.setTab(StockAlertTab.WAREHOUSE);
        }
        boolean cost = permissionService.hasPerm(WmsStockDocService.COST_PERM);
        boolean purchase = pricingService.canViewPurchase();
        boolean sales = salesVisible();
        q.resolve(q.getDate().minusMonths(1).plusDays(1), cost);
        Set<Integer> levels = sales ? priceLevelService.enabledLevels() : Set.of();
        boolean gap = q.getTab() != StockAlertTab.WAREHOUSE;
        Sort order = stockAlertSorts(q.getTab(), cost, purchase, levels).resolve(sort);

        Page<JxcViews.StockAlertRow> page = jxcMapper.selectStockAlert(Page.of(current, size), q, order);
        for (JxcViews.StockAlertRow r : page.getRecords()) {
            r.setStockAmount(cost ? Decimals.money(Decimals.nz(r.getOwnQty()).multiply(Decimals.nz(r.getUnitCost()))) : null);
            r.setUnitCost(cost ? r.getUnitCost() : null);
            if (!purchase) {
                r.setRefCost(null);
            }
            for (int level = PriceLevels.RETAIL; level <= PriceLevels.MAX; level++) {
                if (!levels.contains(level)) {
                    clearPrice(r, level);
                }
            }
            if (gap) {
                r.setRatio(ratio(r.getGapQty(), r.getBaseQty()));
                r.setGapAmount(cost ? Decimals.money(Decimals.nz(r.getGapQty()).multiply(Decimals.nz(r.getUnitCost()))) : null);
            }
        }
        JxcViews.StockAlertRow summary = jxcMapper.selectStockAlertTotal(q);
        if (summary == null) {
            summary = new JxcViews.StockAlertRow();
        }
        summary.setStockAmount(cost ? Decimals.money(summary.getStockAmount()) : null);
        summary.setGapAmount(cost && gap ? Decimals.money(summary.getGapAmount()) : null);
        return new JxcViews.StockAlertPage(page.getRecords(), page.getTotal(), summary, q.getTab(), q.getDate(), q.getMonthBegin(),
                cost, purchase, sales);
    }

    /**
     * 库存预警可排序的列（按标签页）：库存金额、均价、资金需要「查看成本金额」，参考进价需要采购查看权限，
     * 零售价、批发价只有返回的（有售价查看权限且已启用的价格列）
     */
    public static Sorts.Fields stockAlertSorts(StockAlertTab tab, boolean cost, boolean purchase, Set<Integer> levels) {
        Sorts.Fields fields = Sorts.fields(GOODS_SORTS).add("warehouseName", "monthQty", "stockQty").addIf(cost, "stockAmount");
        if (tab != StockAlertTab.WAREHOUSE) {
            return fields.add("baseQty", "ratio", "gapQty").addIf(cost, "gapAmount");
        }
        fields.add("createDate", "lastInDate", "lastSaleDate", "orderQty", "maxQty", "minQty", "locationCode")
                .addIf(cost, "unitCost").addIf(purchase, "refCost");
        levels.stream().sorted().forEach(level -> fields.add(level == PriceLevels.RETAIL ? "retailPrice" : "wholesalePrice" + level));
        return fields;
    }

    /** 积压 / 补货系数 = 数量 ÷ 基数，基数不大于 0 时为空 */
    static BigDecimal ratio(BigDecimal qty, BigDecimal base) {
        if (base == null || base.signum() <= 0) {
            return null;
        }
        return Decimals.nz(qty).divide(base, RATE_SCALE, RoundingMode.HALF_UP);
    }

    private static void clearPrice(JxcViews.StockAlertRow r, int level) {
        switch (level) {
            case 0 -> r.setRetailPrice(null);
            case 1 -> r.setWholesalePrice1(null);
            case 2 -> r.setWholesalePrice2(null);
            case 3 -> r.setWholesalePrice3(null);
            case 4 -> r.setWholesalePrice4(null);
            case 5 -> r.setWholesalePrice5(null);
            case 6 -> r.setWholesalePrice6(null);
            case 7 -> r.setWholesalePrice7(null);
            default -> r.setWholesalePrice8(null);
        }
    }

    /** 售价（零售价、批发价）：报价 / 销售订单 / 销售出库的查看权限，或「商品价格」页面权限（同商品速查） */
    private boolean salesVisible() {
        return pricingService.canViewSales() || permissionService.hasPerm(GOODS_PRICE_PERM);
    }
}
