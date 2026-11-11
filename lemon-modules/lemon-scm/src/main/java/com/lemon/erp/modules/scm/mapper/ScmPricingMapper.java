package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.dto.PricingViews;
import com.lemon.erp.modules.scm.dto.QuickLookupViews;
import com.lemon.erp.modules.scm.model.TradeKind;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 价格参考的查询：报价历史、出入库历史、按供应商汇总的进货数量、按出入库单计算的供商 / 客户对比
 */
@Mapper
public interface ScmPricingMapper {

    List<PricingViews.HistoryRow> selectQuotationHistory(@Param("q") PricingViews.ReferenceQuery q);

    /**
     * @param kind  销售出库或采购入库
     * @param sales 是否销售（往来单位为客户）
     */
    List<PricingViews.HistoryRow> selectTradeHistory(@Param("kind") TradeKind kind, @Param("sales") boolean sales,
                                                     @Param("q") PricingViews.ReferenceQuery q);

    List<PricingViews.PartnerQtyRow> selectPurchaseQty(@Param("q") PricingViews.ReferenceQuery q);

    /**
     * 供商 / 客户对比：按出入库单计算的最新 / 最低 / 最高价（口径同价格记忆），在数据权限规则下调用
     *
     * @param kind  销售出库或采购入库
     * @param sales 是否销售（往来单位为客户）
     */
    List<PricingViews.MemoryRow> selectTradeCompare(@Param("kind") TradeKind kind, @Param("sales") boolean sales,
                                                    @Param("q") PricingViews.ReferenceQuery q);

    // ---------------------------------------------------------------- 商品速查（在出入库单的数据权限规则下调用）

    /** 历史价格的排序：最近的在前 / 最高价在前 / 最低价在前（同价时最近的在前） */
    enum LineOrder { LATEST, HIGHEST, LOWEST }

    /**
     * 各物料最近一次出入库的单价（优惠前，大于 0 的行）
     *
     * @param kind 销售出库或采购入库
     */
    List<QuickLookupViews.LatestPrice> selectLatestPrices(@Param("kind") TradeKind kind, @Param("materialIds") Collection<Long> materialIds);

    /**
     * 物料的出入库价格明细（优惠前单价大于 0 的行），按 order 排序取前 limit 行
     *
     * @param sales 是否销售（往来单位为客户）
     */
    List<QuickLookupViews.TradeLine> selectTradeLines(@Param("kind") TradeKind kind, @Param("sales") boolean sales,
                                                      @Param("q") QuickLookupViews.TradeQuery q, @Param("order") LineOrder order,
                                                      @Param("limit") int limit);

    /** 往来明细分页（最近的在前） */
    IPage<QuickLookupViews.TradeLine> selectTradeLinePage(IPage<QuickLookupViews.TradeLine> page, @Param("kind") TradeKind kind,
                                                          @Param("sales") boolean sales, @Param("q") QuickLookupViews.TradeQuery q);
}
