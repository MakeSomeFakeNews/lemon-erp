package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 发票勾稽的应收应付单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_invoice_item")
public class FinInvoiceItem extends BaseEntity {

    private Long invoiceId;

    private Long billId;

    /** 勾稽金额 */
    private BigDecimal amount;

    @TableField(exist = false)
    private String billNo;
    @TableField(exist = false)
    private String sourceNo;
    @TableField(exist = false)
    private BigDecimal billAmount;
}
