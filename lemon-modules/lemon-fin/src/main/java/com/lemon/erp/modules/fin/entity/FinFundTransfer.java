package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 账户调拨：银行账户之间、银行与现金之间转账
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_fund_transfer")
public class FinFundTransfer extends BaseEntity {

    private String transferNo;

    private LocalDate transferDate;

    private Long fromAccountId;

    private Long toAccountId;

    private BigDecimal amount;

    /** 手续费，由转出账户承担，计入财务费用 */
    private BigDecimal fee;

    private Long voucherId;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String fromAccountName;
    @TableField(exist = false)
    private String toAccountName;
    @TableField(exist = false)
    private String voucherNo;
    @TableField(exist = false)
    private String createByName;
}
