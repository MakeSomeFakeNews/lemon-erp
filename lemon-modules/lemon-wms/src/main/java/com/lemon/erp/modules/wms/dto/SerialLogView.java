package com.lemon.erp.modules.wms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SerialLogView {
    private Long id;
    private String bizType;
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
