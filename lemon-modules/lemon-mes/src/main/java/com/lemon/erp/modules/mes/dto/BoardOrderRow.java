package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import com.lemon.erp.modules.mes.model.WorkOrderType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生产看板：在制工单（含来源销售订单、客户、交期）
 */
@Data
public class BoardOrderRow {
    private Long id;
    private String woNo;
    private WorkOrderType woType;
    private WorkOrderStatus status;
    private BigDecimal qty;
    private BigDecimal finishedQty;
    private LocalDate planStart;
    private LocalDate planEnd;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private String salesOrderNo;
    private String customerName;
    /** 交期：生产计划的需求日期，没有时为销售订单交货日期 */
    private LocalDate dueDate;
    private LocalDateTime lastReportTime;
}
