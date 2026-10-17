package com.lemon.erp.modules.fin.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.fin.model.ExpenseClaimStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 费用报销单：草稿 → 审批中 → 待付款 → 已付款
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_expense_claim")
public class FinExpenseClaim extends BaseEntity {

    private String claimNo;

    /** 报销人 */
    private Long userId;

    /** 费用部门 */
    private Long deptId;

    private LocalDate claimDate;

    private BigDecimal amount;

    /** 冲抵的借款 */
    private Long loanId;

    /** 冲借款金额 */
    private BigDecimal loanOffset;

    private ExpenseClaimStatus status;

    private Long payAccountId;

    private LocalDate payDate;

    private Long voucherId;

    private String flowInstanceId;

    /** 事由 */
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String loanNo;
    @TableField(exist = false)
    private String voucherNo;
    @TableField(exist = false)
    private List<FinExpenseClaimItem> items;
}
