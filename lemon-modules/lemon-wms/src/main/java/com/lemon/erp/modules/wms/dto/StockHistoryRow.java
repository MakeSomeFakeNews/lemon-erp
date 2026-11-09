package com.lemon.erp.modules.wms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.wms.model.StockBizType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 经营历史的一行：一条库存流水，带结余数量、结余金额
 */
@Data
public class StockHistoryRow {
    private Long id;
    private LocalDate bizDate;
    /** 库存业务类型：入库 / 出库 / 调拨 / 盘点 */
    private StockBizType bizType;
    /** 出入库单的入库类型 / 出库类型 */
    private String docBizType;
    private Long docId;
    private String docNo;
    /** 来源单号（采购入库、销售出库等业务单据） */
    private String sourceNo;
    private String warehouseName;
    private String batchNo;
    private BigDecimal qty;
    private BigDecimal bizPrice;
    private BigDecimal costPrice;
    private BigDecimal costAmount;
    private String partnerName;
    /** 1 为升级前的历史流水，成本是回填的近似值 */
    private Integer costEstimated;
    /** 结余数量 */
    private BigDecimal balanceQty;
    /** 结余金额 */
    private BigDecimal balanceAmount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
