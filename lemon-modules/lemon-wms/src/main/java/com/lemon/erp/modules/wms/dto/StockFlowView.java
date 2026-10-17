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
    private String createByName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
