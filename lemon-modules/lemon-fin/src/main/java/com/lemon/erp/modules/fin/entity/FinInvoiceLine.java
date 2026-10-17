package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 发票明细行（导入数电票时保存）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_invoice_line")
public class FinInvoiceLine extends BaseEntity {

    private Long invoiceId;

    private Integer seq;

    private String itemName;

    private String spec;

    private String unit;

    private BigDecimal qty;

    private BigDecimal price;

    private BigDecimal amount;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;
}
