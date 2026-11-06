package com.lemon.erp.modules.scm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.modules.scm.model.PriceSource;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 开单默认价、价格参考、价格记忆的查询结果
 */
public final class PricingViews {

    private PricingViews() {
    }

    /**
     * 开单默认价（含税、基本单位、本位币）
     *
     * @param price   没有可用价格时为 null
     * @param taxRate 来源没有税率（价格列、最近价、参考成本）时为 null，表单保持原税率
     * @param source  来源，没有价格时为 null
     */
    public record DefaultPrice(Long materialId, BigDecimal price, BigDecimal taxRate, PriceSource source) {
    }

    /** 报价 / 销售 / 进货历史的一行 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HistoryRow {
        private Long docId;
        private String docNo;
        private LocalDate bizDate;
        private Long partnerId;
        private String partnerCode;
        private String partnerName;
        private BigDecimal qty;
        private BigDecimal price;
        private BigDecimal amount;
        private String remark;
    }

    /** 进货数量：按供应商汇总 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PartnerQtyRow {
        private Long partnerId;
        private String partnerCode;
        private String partnerName;
        private Integer times;
        private BigDecimal qty;
        private BigDecimal amount;
        private BigDecimal avgPrice;
        private LocalDate lastDate;
    }

    /** 供商 / 客户对比：价格记忆 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MemoryRow {
        private Long partnerId;
        private String partnerCode;
        private String partnerName;
        private LocalDate lastDate;
        private BigDecimal lastPrice;
        private LocalDate minDate;
        private BigDecimal minPrice;
        private LocalDate maxDate;
        private BigDecimal maxPrice;
    }

    /**
     * 价格参考：没有权限的分区为 null（不输出），有权限但没有数据为空列表
     *
     * @param quotations      报价历史（销售）
     * @param sales           销售历史（销售出库）
     * @param purchases       进货历史（采购入库）
     * @param purchaseQty     进货数量（按供应商汇总）
     * @param supplierCompare 供商对比（采购价格记忆）
     * @param customerCompare 客户对比（销售价格记忆）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Reference(List<HistoryRow> quotations, List<HistoryRow> sales, List<HistoryRow> purchases,
                            List<PartnerQtyRow> purchaseQty, List<MemoryRow> supplierCompare, List<MemoryRow> customerCompare) {
    }

    /** 价格参考的查询条件 */
    public record ReferenceQuery(Long materialId, Long partnerId, String keyword, LocalDate start, LocalDate end, int limit) {
    }
}
