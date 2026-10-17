package com.lemon.erp.modules.fin.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 银行对账单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_bank_statement")
public class FinBankStatement extends BaseEntity {

    /** 银行存款科目 */
    private Long accountId;

    private LocalDate txnDate;

    /** 收入为正、支出为负 */
    private BigDecimal amount;

    private String summary;

    private String refNo;

    /** 勾对的凭证分录 */
    private Long entryId;

    private String batchNo;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String voucherNo;
    @TableField(exist = false)
    private LocalDate voucherDate;
}
