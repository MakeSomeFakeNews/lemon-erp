package com.lemon.erp.modules.fin.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.FeeBearer;
import com.lemon.erp.modules.fin.model.VoucherStatus;
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

    /** 手续费支付账户：FROM 转出账户支付（转出 金额 + 手续费）/ TO 转入账户支付（转入 金额 - 手续费），手续费为 0 时不起作用 */
    private FeeBearer feeBearer;

    /** 经办人（用户），可空 */
    private Long handlerId;

    /** 票据号码（转账回单、支票号等），可空 */
    private String ticketNo;

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
    private VoucherStatus voucherStatus;
    @TableField(exist = false)
    private String handlerName;
    @TableField(exist = false)
    private String createByName;
}
