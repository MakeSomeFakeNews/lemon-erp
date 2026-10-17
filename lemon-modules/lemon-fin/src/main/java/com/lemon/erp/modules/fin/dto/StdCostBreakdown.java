package com.lemon.erp.modules.fin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.modules.fin.entity.FinStdCost;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 自制件标准成本的卷算明细：标准成本、BOM 子件的用量与单价、工序的人工和制造费用
 */
public record StdCostBreakdown(FinStdCost row, List<Part> items, List<BomOp> ops) {

    /**
     * 子件
     *
     * @param make    1 自制件
     * @param unitQty 单位用量（含损耗）
     */
    public record Part(Long materialId, String materialCode, String materialName, String unit, Integer make, BigDecimal qty,
                       BigDecimal lossRate, BigDecimal unitQty, BigDecimal unitCost, BigDecimal amount) {
    }

    /** BOM 子件（数据库查询结果） */
    @Data
    public static class BomItem {
        private Long bomId;
        private Long materialId;
        private BigDecimal qty;
        /** 损耗率（%） */
        private BigDecimal lossRate;
    }

    /** BOM 工序及工作中心费率（数据库查询结果，为空的列不输出）；labor、overhead 为按标准工时计算的人工、制造费用 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BomOp {
        private Long bomId;
        private Integer seq;
        private String name;
        private String workCenterName;
        private BigDecimal stdHours;
        private BigDecimal laborRate;
        private BigDecimal overheadRate;
        private BigDecimal labor;
        private BigDecimal overhead;
    }

    /** 没有标准单价的采购件的初始单价：最近一次核算的单价，没有时为参考成本价（数据库查询结果） */
    @Data
    public static class InitialPrice {
        private Long materialId;
        private BigDecimal price;
    }
}
