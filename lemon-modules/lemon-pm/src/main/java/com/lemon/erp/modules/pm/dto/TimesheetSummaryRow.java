package com.lemon.erp.modules.pm.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 工时汇总的一组（项目或成员）：已通过、待审批的小时数，填报人数 / 项目数
 */
@Data
public class TimesheetSummaryRow {

    private String groupCode;
    private String groupName;
    private BigDecimal approvedHours;
    private BigDecimal pendingHours;
    private Long peopleCount;
    private Long projectCount;
}
