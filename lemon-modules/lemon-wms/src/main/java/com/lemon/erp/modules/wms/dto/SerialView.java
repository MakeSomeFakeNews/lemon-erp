package com.lemon.erp.modules.wms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.wms.model.SerialStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SerialView {
    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String serialNo;
    private Long warehouseId;
    private String warehouseName;
    private Long locationId;
    private String locationCode;
    private String batchNo;
    private SerialStatus status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
