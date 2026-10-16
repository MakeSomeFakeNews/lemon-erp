package com.lemon.erp.modules.fin.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 辅助核算余额：按项目 / 客户 / 供应商 + 科目汇总的本期发生额和期末余额（借 - 贷）
 */
@Data
public class AuxBalanceRow {
    private Long auxId;
    private String auxCode;
    private String auxName;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;
}
