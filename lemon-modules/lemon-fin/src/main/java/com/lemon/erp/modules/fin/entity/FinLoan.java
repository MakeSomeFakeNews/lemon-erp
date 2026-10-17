package com.lemon.erp.modules.fin.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.fin.model.LoanStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 员工借款（备用金）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_loan")
public class FinLoan extends BaseEntity {

    private String loanNo;

    /** 借款人 */
    private Long userId;

    private Long deptId;

    private LocalDate loanDate;

    private BigDecimal amount;

    /** 已还款（含报销冲抵） */
    private BigDecimal repaidAmount;

    private LoanStatus status;

    private Long payAccountId;

    private Long voucherId;

    private String flowInstanceId;

    private String purpose;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String voucherNo;
    @TableField(exist = false)
    private List<FinLoanRepay> repays;
}
