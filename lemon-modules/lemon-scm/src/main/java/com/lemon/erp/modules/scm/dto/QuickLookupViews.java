package com.lemon.erp.modules.scm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.QuickPrice;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 商品速查（issue #97）的查询结果。价格均为含税、基本单位、本位币
 */
public final class QuickLookupViews {

    private QuickLookupViews() {
    }

    /**
     * 可选的价格（「查询价格」设置）
     *
     * @param type    价格种类
     * @param label   显示名称（价格列取「价格列设置」中的名称）
     * @param side    所属方面：PURCHASE 进货类 / COST 成本 / SALES 售价类，不显示价格时为空
     * @param allowed 当前用户是否有权限查看（没有权限时即使选择也不返回价格）
     */
    public record PriceOption(QuickPrice type, String label, QuickPrice.Side side, boolean allowed) {
    }

    /**
     * 速查结果的一行
     *
     * @param imageUrl 商品主图（公开文件地址，没有图片时为空），全部图片用 POST /wms/material/images/urls 获取
     * @param qty      全部仓库的可用库存（库存 − 冻结）
     * @param price    所选价格，不显示价格、没有权限或没有数据时为空
     */
    public record Row(Long materialId, String code, String name, String spec, String unit, String imageUrl, BigDecimal qty, BigDecimal price) {
    }

    /**
     * 速查结果的一页（不查总数）
     *
     * @param price       实际显示的价格种类；选择了没有权限或已停用的价格列时为空（只显示数量）
     * @param priceDenied 选择的价格没有权限查看或已停用
     * @param priceLabel  显示的价格名称
     * @param hasMore     可能还有下一页
     */
    public record Page(List<Row> rows, QuickPrice price, String priceLabel, boolean priceDenied, boolean hasMore) {
    }

    /** 历史价格的一笔：日期、往来单位、数量、单价（优惠前） */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TradeLine {
        private Long docId;
        private String docNo;
        private LocalDate bizDate;
        private Long partnerId;
        private String partnerCode;
        private String partnerName;
        private BigDecimal qty;
        private BigDecimal price;
        /** 制单人 */
        private String createByName;
        /** 复核（审核）人 */
        private String postByName;
    }

    /**
     * 售价 / 进价历史参考（按出入库单的数据权限统计，单价为优惠前单价，不含赠品等 0 价行）
     *
     * @param kind   SALES 销售出库 / PURCHASE 采购入库
     * @param recent 最近 3 次（指定往来单位时为该单位的最近 3 次）
     * @param latest 最新价（全部往来单位）
     * @param max    最高价（同价取最近的一次）
     * @param min    最低价（同价取最近的一次）
     */
    public record PriceSummary(OrderKind kind, List<TradeLine> recent, TradeLine latest, TradeLine max, TradeLine min) {
    }

    /** 各物料的最新价（速查列表的最新进价 / 最新售价） */
    @Data
    public static class LatestPrice {
        private Long materialId;
        private BigDecimal price;
    }

    /**
     * 往来明细（更多交易历史）的查询条件
     *
     * @param keyword 往来单位编码、名称或电话
     */
    public record TradeQuery(Long materialId, Long partnerId, String keyword, LocalDate start, LocalDate end) {
    }
}
