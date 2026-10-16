package com.lemon.erp.modules.fin.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 科目借贷发生额汇总（数据库 GROUP BY 结果）
 */
@Data
public class AccountSum {

    private Long accountId;
    private Integer period;
    private Long deptId;
    private BigDecimal debit;
    private BigDecimal credit;
}
