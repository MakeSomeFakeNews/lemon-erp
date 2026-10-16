package com.lemon.erp.modules.fin.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 科目余额表一行：期初、本期借贷发生额、期末，余额按科目方向（借方科目 = 借 - 贷）
 */
@Data
public class BalanceRow {

    private Long accountId;
    private String code;
    private String name;
    private String category;
    private String direction;
    private Integer level;
    private Boolean leaf;

    private BigDecimal openBalance = BigDecimal.ZERO;
    private BigDecimal debit = BigDecimal.ZERO;
    private BigDecimal credit = BigDecimal.ZERO;
    private BigDecimal closeBalance = BigDecimal.ZERO;
}
