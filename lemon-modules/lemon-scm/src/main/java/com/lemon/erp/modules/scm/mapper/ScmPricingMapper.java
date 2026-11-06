package com.lemon.erp.modules.scm.mapper;

import com.lemon.erp.modules.scm.dto.PricingViews;
import com.lemon.erp.modules.scm.model.TradeKind;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 价格参考的查询：报价历史、出入库历史、按供应商汇总的进货数量
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
}
