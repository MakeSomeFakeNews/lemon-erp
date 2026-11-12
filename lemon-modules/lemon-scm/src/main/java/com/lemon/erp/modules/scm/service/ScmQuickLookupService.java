package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.modules.scm.dto.QuickLookupViews;
import com.lemon.erp.modules.scm.dto.QuickLookupViews.PriceOption;
import com.lemon.erp.modules.scm.dto.QuickLookupViews.TradeLine;
import com.lemon.erp.modules.scm.dto.QuickLookupViews.TradeQuery;
import com.lemon.erp.modules.scm.mapper.ScmPricingMapper;
import com.lemon.erp.modules.scm.mapper.ScmPricingMapper.LineOrder;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.QuickPrice;
import com.lemon.erp.modules.scm.model.ScmConstants;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.dto.StockQuery;
import com.lemon.erp.modules.wms.dto.StockView;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsPriceLevel;
import com.lemon.erp.modules.wms.model.PriceLevels;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsPriceLevelService;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import com.lemon.erp.modules.wms.service.WmsStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品速查（issue #97）：按名称、规格、简拼等搜索商品，显示可用库存和用户选择的一种价格；
 * 联查库存分布（仓库、库位、批次）、售价 / 进价历史参考（最近 3 次、最新、最高、最低）和往来明细。
 * <p>价格是否可见按权限在这里过滤（不只是前端隐藏），规则与价格参考（{@link ScmPricingService#reference}）一致：
 * 进货类需要采购订单 / 采购入库的查看权限，库存均价需要「查看成本金额」，售价类需要报价 / 销售订单 / 销售出库的查看权限，
 * 零售价、批发价还可以由「商品价格」页面权限查看。最新价、历史价按出入库单的数据权限统计（只来自看得到的单据）
 */
@Service
@RequiredArgsConstructor
public class ScmQuickLookupService {

    /** 历史参考：最近几次 */
    public static final int RECENT_TIMES = 3;
    /** 库存分布最多返回的行数 */
    static final int STOCK_LIMIT = 200;

    private final ScmPricingService pricingService;
    private final ScmPricingMapper pricingMapper;
    private final WmsMaterialService materialService;
    private final WmsPriceLevelService priceLevelService;
    private final WmsStockQueryService stockQueryService;
    private final PermissionService permissionService;

    // ---------------------------------------------------------------- 价格权限

    /** 当前用户能否查看某种价格（NONE 总是可以）；价格列还要求已启用 */
    public boolean allowed(QuickPrice type) {
        if (type == QuickPrice.NONE) {
            return true;
        }
        if (type.level() != null && !priceLevelService.isEnabled(type.level())) {
            return false;
        }
        return switch (type.side()) {
            case PURCHASE -> pricingService.canViewPurchase();
            case COST -> permissionService.hasPerm(WmsStockDocService.COST_PERM);
            case SALES -> pricingService.canViewSales() || (type.level() != null && permissionService.hasPerm("scm:goodsPrice:list"));
        };
    }

    /** 可选的价格：停用的价格列不列出，没有权限的标记为不可选 */
    public List<PriceOption> priceOptions() {
        Map<Integer, String> levelNames = priceLevelService.list().stream()
                .collect(Collectors.toMap(WmsPriceLevel::getLevel, WmsPriceLevel::getName, (a, b) -> a));
        List<PriceOption> options = new ArrayList<>();
        for (QuickPrice type : QuickPrice.values()) {
            if (type.level() != null && !priceLevelService.isEnabled(type.level())) {
                continue;
            }
            options.add(new PriceOption(type, label(type, levelNames), type.side(), allowed(type)));
        }
        return options;
    }

    private static String label(QuickPrice type, Map<Integer, String> levelNames) {
        if (type.level() != null) {
            String name = levelNames.get(type.level());
            return name == null ? WmsPriceLevelService.defaultName(type.level()) : name;
        }
        return Messages.get(switch (type) {
            case REF_COST -> "scm.label.quickPrice.REF_COST";
            case AVG_COST -> "scm.label.quickPrice.AVG_COST";
            case LAST_PURCHASE -> "scm.label.quickPrice.LAST_PURCHASE";
            case LAST_SALES -> "scm.label.quickPrice.LAST_SALES";
            default -> "scm.label.quickPrice.NONE";
        });
    }

    // ---------------------------------------------------------------- 速查列表

    /**
     * 搜索商品（启用的、非多规格款式，规则同物料下拉：编码、名称、规格、条码、拼音首字母、去掉分隔符的缩写），不查总数
     *
     * @param price 显示的价格，为空时按最新售价；没有权限或已停用时不返回价格（priceDenied）
     * @param size  每页条数，最多 {@value WmsMaterialService#OPTION_LIMIT}
     */
    public QuickLookupViews.Page search(String keyword, QuickPrice price, long current, long size) {
        QuickPrice type = price == null ? QuickPrice.LAST_SALES : price;
        boolean allowed = allowed(type);
        QuickPrice shown = allowed && type != QuickPrice.NONE ? type : null;
        long pageSize = Math.min(Math.max(size, 1), WmsMaterialService.OPTION_LIMIT);
        List<WmsMaterial> found = materialService.options(keyword, current, pageSize);
        List<Long> ids = found.stream().map(WmsMaterial::getId).toList();
        Map<Long, BigDecimal> qty = stockQueryService.available(ids);
        Map<Long, BigDecimal> prices = shown == null ? Map.of() : prices(shown, ids);
        List<QuickLookupViews.Row> rows = found.stream()
                .map(m -> new QuickLookupViews.Row(m.getId(), m.getCode(), m.getName(), m.getSpec(), m.getUnit(), m.getImageUrl(),
                        qty.getOrDefault(m.getId(), BigDecimal.ZERO), prices.get(m.getId())))
                .toList();
        String label = shown == null ? null : priceOptions().stream().filter(o -> o.type() == shown)
                .map(PriceOption::label).findFirst().orElse(null);
        return new QuickLookupViews.Page(rows, shown, label, !allowed, found.size() >= pageSize);
    }

    /** 各物料的某种价格（调用前已校验权限） */
    private Map<Long, BigDecimal> prices(QuickPrice type, List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return switch (type) {
            case AVG_COST -> stockQueryService.unitCosts(ids);
            case LAST_PURCHASE -> latestPrices(TradeKind.PURCHASE_IN, ids);
            case LAST_SALES -> latestPrices(TradeKind.SALES_OUT, ids);
            default -> {
                Map<Long, BigDecimal> map = new HashMap<>();
                materialService.mapByIds(ids).forEach((id, m) -> {
                    BigDecimal p = type == QuickPrice.REF_COST ? m.getRefCost() : PriceLevels.price(m, type.level());
                    if (p != null) {
                        map.put(id, p);
                    }
                });
                yield map;
            }
        };
    }

    private Map<Long, BigDecimal> latestPrices(TradeKind kind, List<Long> ids) {
        return DataPermissionContext.execute(ScmConstants.TRADE_RULE, () -> pricingMapper.selectLatestPrices(kind, ids)).stream()
                .collect(Collectors.toMap(QuickLookupViews.LatestPrice::getMaterialId, QuickLookupViews.LatestPrice::getPrice,
                        (a, b) -> a));
    }

    // ---------------------------------------------------------------- 联查

    /** 库存分布：各仓库、库位、批次的数量（不显示 0 库存；成本单价、金额按「查看成本金额」权限返回） */
    public List<StockView> stock(Long materialId) {
        return stockQueryService.stockPage(1, STOCK_LIMIT, new StockQuery(null, null, materialId, null, null, null, false, null, null)).records();
    }

    /**
     * 售价 / 进价历史参考：最近 3 次、最新、最高、最低（没有对应权限时报错）
     *
     * @param partnerId 往来单位（客户 / 供应商，与 kind 一致），指定时最近 3 次只看该单位，最新 / 最高 / 最低仍为全部单位
     */
    public QuickLookupViews.PriceSummary summary(OrderKind kind, Long materialId, Long partnerId) {
        checkKind(kind);
        TradeQuery all = new TradeQuery(materialId, null, null, null, null);
        TradeQuery mine = partnerId == null ? all : new TradeQuery(materialId, partnerId, null, null, null);
        List<TradeLine> recent = lines(kind, mine, LineOrder.LATEST, RECENT_TIMES);
        TradeLine latest = partnerId == null ? first(recent) : first(lines(kind, all, LineOrder.LATEST, 1));
        return new QuickLookupViews.PriceSummary(kind, recent, latest,
                first(lines(kind, all, LineOrder.HIGHEST, 1)), first(lines(kind, all, LineOrder.LOWEST, 1)));
    }

    /** 往来明细（更多交易历史）：日期、数量、单价、往来单位、制单、复核，最近的在前 */
    public PageResult<TradeLine> trades(OrderKind kind, TradeQuery query, long current, long size) {
        checkKind(kind);
        Page<TradeLine> page = Page.of(current, size);
        boolean sales = kind == OrderKind.SALES;
        DataPermissionContext.execute(ScmConstants.TRADE_RULE, () -> pricingMapper.selectTradeLinePage(page, tradeKind(kind), sales, query));
        return PageResult.of(page);
    }

    private List<TradeLine> lines(OrderKind kind, TradeQuery q, LineOrder order, int limit) {
        return DataPermissionContext.execute(ScmConstants.TRADE_RULE,
                () -> pricingMapper.selectTradeLines(tradeKind(kind), kind == OrderKind.SALES, q, order, limit));
    }

    private void checkKind(OrderKind kind) {
        boolean ok = kind == OrderKind.SALES ? pricingService.canViewSales() : pricingService.canViewPurchase();
        if (!ok) {
            throw new BizException(kind == OrderKind.SALES ? "scm.error.noPermissionSalesPrice" : "scm.error.noPermissionPurchasePrice");
        }
    }

    private static TradeKind tradeKind(OrderKind kind) {
        return kind == OrderKind.SALES ? TradeKind.SALES_OUT : TradeKind.PURCHASE_IN;
    }

    private static <T> T first(List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }
}
