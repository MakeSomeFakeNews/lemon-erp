package com.lemon.erp.modules.wms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.wms.model.StockBizType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SerialLogView {
    private Long id;
    /** 库存业务类型：入库 / 出库 / 调拨 / 盘点 */
    private StockBizType bizType;
    private Long docId;
    private String docNo;
    /** 1 入 -1 出 */
    private Integer direction;
    private String warehouseName;
    private String locationCode;
    private String batchNo;
    private String createByName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
