package com.lemon.erp.modules.scm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 价格、促销相关的查询结果。数据库查询结果用带 setter 的类（MyBatis 按列名映射，为空的列不输出，与原先返回 Map 时一致），
 * 在 Java 中组装的结构用 record
 */
public final class ScmPriceViews {

    private ScmPriceViews() {
    }

    /** 历史成交价：已审核订单（不含赠品）的原价（没有促销时为成交价） */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderPriceHistory {
        private Long orderId;
        private String orderNo;
        private LocalDate orderDate;
        private BigDecimal price;
        private BigDecimal taxRate;
        private BigDecimal qty;
        private String currency;
        private BigDecimal exchangeRate;
    }

    /** 物料的采购入库记录（最近 100 条） */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PurchaseHistory {
        private LocalDate bizDate;
        private String docNo;
        private Long supplierId;
        private String supplierName;
        private Long materialId;
        private BigDecimal qty;
        private BigDecimal price;
        private BigDecimal taxRate;
    }

    /** 促销活动的使用记录 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PromotionUsage {
        private Long orderId;
        private String orderNo;
        private String customerName;
        private LocalDate orderDate;
        private String status;
        private BigDecimal discountAmount;
        private BigDecimal giftQty;
    }

    /** 促销试算结果：订单行的促销价、赠品、整单优惠金额、说明 */
    public record PromotionPreview(List<PreviewLine> lines, List<PreviewGift> gifts, BigDecimal discountAmount, List<String> notes) {
    }

    /** 订单行：促销后的单价、原价、适用的促销 */
    public record PreviewLine(Long materialId, BigDecimal price, BigDecimal origPrice, String promotionName) {
    }

    /**
     * 赠品行
     *
     * @param materialName  物料编码 + 名称
     * @param qty           赠品数量（基本单位）
     * @param promotionId   买赠促销
     * @param batchEnabled  批次管理（1 是），直接开单出库时需要选批次
     * @param serialEnabled 序列号管理（1 是），直接开单出库时需要填写序列号
     * @param isVirtual     虚拟商品（服务项目，1 是），出库不选库位批次
     */
    public record PreviewGift(Long materialId, BigDecimal price, BigDecimal origPrice, String promotionName, String materialName,
                              BigDecimal qty, Long promotionId, String materialCode, String spec, String unit, Integer batchEnabled,
                              Integer serialEnabled, Integer isVirtual) {
    }

    /** 经销商订货目录：物料、基础价格、税率、起订数量阶梯价 */
    public record DealerCatalogItem(Long materialId, String materialCode, String materialName, String spec, String unit,
                                    BigDecimal price, BigDecimal taxRate, List<PriceTier> tiers) {
    }

    /** 起订数量阶梯价 */
    public record PriceTier(BigDecimal minQty, BigDecimal price) {
    }
}
