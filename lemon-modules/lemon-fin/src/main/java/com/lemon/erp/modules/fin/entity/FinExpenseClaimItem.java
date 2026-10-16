package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 费用报销明细
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_expense_claim_item")
public class FinExpenseClaimItem extends BaseEntity {

    private Long claimId;

    private LocalDate expenseDate;

    /** 费用科目 */
    private Long accountId;

    private Long projectId;

    private BigDecimal amount;

    private String remark;

    @TableField(exist = false)
    private String accountCode;
    @TableField(exist = false)
    private String accountName;
}
