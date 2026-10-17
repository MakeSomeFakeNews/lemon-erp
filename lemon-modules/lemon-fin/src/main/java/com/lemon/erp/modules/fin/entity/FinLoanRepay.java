package com.lemon.erp.modules.fin.entity;

import com.lemon.erp.modules.fin.model.LoanRepayMethod;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 借款还款记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_loan_repay")
public class FinLoanRepay extends BaseEntity {

    private Long loanId;

    private LocalDate repayDate;

    private BigDecimal amount;

    /** CASH 现金还款 / CLAIM 报销冲抵 */
    private LoanRepayMethod method;

    private Long claimId;

    private Long accountId;

    private Long voucherId;

    private Long createBy;

    @TableField(exist = false)
    private String claimNo;
    @TableField(exist = false)
    private String voucherNo;
}
