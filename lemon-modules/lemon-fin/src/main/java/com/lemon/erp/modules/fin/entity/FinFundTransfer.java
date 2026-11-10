package com.lemon.erp.modules.fin.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.FeeBearer;
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

    /** 手续费，计入财务费用 */
    private BigDecimal fee;

    /** 手续费承担方：FROM 转出账户（转出 金额 + 手续费）/ TO 转入账户（转入 金额 - 手续费） */
    private FeeBearer feeBearer;

    private Long voucherId;

    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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
