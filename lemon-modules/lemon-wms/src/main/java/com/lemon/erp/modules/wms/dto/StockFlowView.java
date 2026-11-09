package com.lemon.erp.modules.wms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.wms.model.StockBizType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockFlowView {
    private Long id;
    /** 库存业务类型：入库 / 出库 / 调拨 / 盘点 */
    private StockBizType bizType;
    private Long docId;
    private String docNo;
    private Long warehouseId;
    private String warehouseName;
    private Long locationId;
    private String locationCode;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private String batchNo;
    private BigDecimal qty;
    /** 业务单价（来源单据的不含税单价），需要「查看成本金额」权限 */
    private BigDecimal bizPrice;
    /** 成本单价，需要「查看成本金额」权限 */
    private BigDecimal costPrice;
    /** 成本金额，需要「查看成本金额」权限 */
    private BigDecimal costAmount;
    /** 1 为升级前的历史流水，成本是按当时的成本核算单价 / 参考成本价回填的近似值 */
    private Integer costEstimated;
    private Long partnerId;
    private String partnerName;
    /** 出入库单的入库类型 / 出库类型 */
    private String docBizType;
    private String createByName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
