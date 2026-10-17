package com.lemon.erp.modules.scm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 波次拣货单：按库位路径汇总的拣货行（含分拣到各发货通知的数量）和库存不足的明细
 */
public record WavePickList(List<Line> lines, List<Shortage> shortages) {

    /** 拣货行：同一库位、物料、批次合并，sorts 为分到各发货通知的数量 */
    public record Line(Long locationId, String locationCode, String zone, Long materialId, String materialCode, String materialName,
                       String spec, String unit, String batchNo, LocalDate expiryDate, BigDecimal qty, List<Sort> sorts) {
    }

    /** 分拣到发货通知 */
    public record Sort(String deliveryNo, String customerName, BigDecimal qty) {
    }

    /** 波次中待拣货发货通知的建议拣货明细（数据库查询结果） */
    @Data
    public static class PickRow {
        private Long deliveryId;
        private String deliveryNo;
        private String customerName;
        private Long deliveryItemId;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String spec;
        private String unit;
        private Integer serialEnabled;
        private Long locationId;
        private String locationCode;
        private String zone;
        private String batchNo;
        private LocalDate expiryDate;
        private BigDecimal qty;
    }

    /** 建议拣货数量不足的发货明细（数据库查询结果，为空的列不输出） */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Shortage {
        private String deliveryNo;
        private String materialCode;
        private String materialName;
        private String unit;
        private BigDecimal qty;
        /** 已建议拣货的数量 */
        private BigDecimal suggested;
    }
}
