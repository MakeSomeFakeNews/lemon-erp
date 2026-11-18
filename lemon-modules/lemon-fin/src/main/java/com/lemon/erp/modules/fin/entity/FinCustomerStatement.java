package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 客户对账单（issue #134）：生成时的往来对账快照，期末 = 期初 + 本期应收 − 收款 − 折让 − 对冲。
 * 单号、期间、状态等公共字段见 {@link FinStatementBase}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_customer_statement")
public class FinCustomerStatement extends FinStatementBase {

    private Long customerId;

    private BigDecimal opening;

    /** 本期应收（应收单价税合计，退货、冲账为负） */
    private BigDecimal billAmount;

    /** 本期收款（收款实收 + 预收款） */
    private BigDecimal receiptAmount;

    /** 本期折让（收款核销时的折让 / 抹零） */
    private BigDecimal discountAmount;

    /** 本期应收应付对冲 */
    private BigDecimal nettingAmount;

    /** 期末余额（客户欠款，负数为预收） */
    private BigDecimal closing;

    private Integer lineCount;

    /** 客户确认或提出异议时的意见 */
    private String customerRemark;

    /** 确认 / 提出异议的门户账号 */
    private Long confirmBy;

    @TableField(exist = false)
    private String customerCode;
    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String confirmByName;
}
