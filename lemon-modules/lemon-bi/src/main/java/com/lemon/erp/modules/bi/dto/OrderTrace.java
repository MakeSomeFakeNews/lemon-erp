package com.lemon.erp.modules.bi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 订单全链路追溯：出入库单、应收应付单、收付款、物流发运、生产计划、项目、来料检验。
 * 没有对应查看权限的部分不返回（销售订单没有来料检验，采购订单没有发运、生产计划、项目）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderTrace(List<TradeDoc> tradeDocs, List<Bill> bills, List<Settlement> settlements, List<Shipment> shipments,
                         List<Plan> plans, List<Project> projects, List<Inspection> inspections) {

    /** 以下为数据库查询结果，为空的列不输出 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TradeDoc {
        private Long id;
        private String docNo;
        private String kind;
        private LocalDate bizDate;
        private BigDecimal totalQty;
        private BigDecimal totalAmount;
        private String wmsDocNo;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Bill {
        private Long id;
        private String billNo;
        private String kind;
        private String sourceNo;
        private LocalDate billDate;
        private BigDecimal amount;
        private BigDecimal settledAmount;
        private String status;
        private Long voucherId;
        private String voucherNo;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Settlement {
        private Long id;
        private String settleNo;
        private String kind;
        private LocalDate settleDate;
        private BigDecimal amount;
        private String status;
        private Long voucherId;
        private String voucherNo;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Shipment {
        private Long id;
        private String shipNo;
        private String carrier;
        private String trackingNo;
        private LocalDate shipDate;
        private String status;
        private String tradeDocNo;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Inspection {
        private Long id;
        private String inspNo;
        private String sourceNo;
        private String materialCode;
        private String materialName;
        private BigDecimal inspectQty;
        private String result;
        private String status;
        private LocalDate inspectDate;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Plan {
        private Long id;
        private String planNo;
        private String materialCode;
        private String materialName;
        private BigDecimal qty;
        private LocalDate dueDate;
        private String status;
        private Long woCount;
        private BigDecimal finishedQty;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Project {
        private Long id;
        private String code;
        private String name;
        private String status;
        private BigDecimal progress;
    }
}
