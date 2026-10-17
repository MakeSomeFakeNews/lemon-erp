package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.AccountDirection;
import com.lemon.erp.modules.fin.model.AccountCategory;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 科目期初余额录入行：全部启用的末级科目
 */
@Data
public class AccountOpeningRow {
    private Long accountId;
    private String code;
    private String name;
    private AccountCategory category;
    private AccountDirection direction;
    private BigDecimal debit;
    private BigDecimal credit;
}
