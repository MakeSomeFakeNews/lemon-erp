package com.lemon.erp.modules.hr.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 请假 / 加班汇总的一组（假期类型或部门）：申请数、人数、小时数
 */
@Data
public class LeaveSummaryRow {

    private String groupName;
    private Long applyCount;
    private Long employeeCount;
    private BigDecimal hours;
}
