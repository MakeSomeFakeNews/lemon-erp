package com.lemon.erp.modules.mes.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产看板：某天的报工汇总，id 为工序或工作中心
 */
@Data
public class BoardReportSum {
    private Long id;
    private BigDecimal good;
    private BigDecimal scrap;
    private BigDecimal hours;
    private LocalDateTime lastTime;
}
