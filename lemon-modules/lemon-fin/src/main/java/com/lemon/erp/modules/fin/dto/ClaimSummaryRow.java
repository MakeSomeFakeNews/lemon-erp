package com.lemon.erp.modules.fin.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 费用报销汇总的一行：按部门或费用科目汇总的报销单数、金额
 */
@Data
public class ClaimSummaryRow {

    private String groupName;
    private Long claimCount;
    private BigDecimal amount;
}
