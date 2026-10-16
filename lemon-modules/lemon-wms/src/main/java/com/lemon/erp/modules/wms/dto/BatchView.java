package com.lemon.erp.modules.wms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BatchView {
    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    /** 当前在库数量（全部仓库） */
    private BigDecimal qty;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
