package com.lemon.erp.modules.fin.entity;

import com.lemon.erp.modules.fin.model.InvoiceStatus;
import com.lemon.erp.modules.fin.model.InvoiceSource;
import com.lemon.erp.modules.fin.model.InvoiceType;
import com.lemon.erp.modules.fin.model.InvoiceKind;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 发票登记：销项（开给客户）、进项（供应商开来），与应收应付单勾稽
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_invoice")
public class FinInvoice extends BaseEntity {

    /** OUTPUT 销项 / INPUT 进项 */
    private InvoiceKind kind;

    private String invoiceNo;

    /** 字典 fin_invoice_type：SPECIAL 专票 / NORMAL 普票 / E_SPECIAL 数电票（专票）/ E_NORMAL 数电票（普票） */
    private InvoiceType invoiceType;

    private Long partnerId;

    private LocalDate invoiceDate;

    /** 价税合计 */
    private BigDecimal amount;

    private BigDecimal taxAmount;

    /** 已勾稽应收应付单的金额 */
    private BigDecimal matchedAmount;

    private String sellerTaxNo;

    private String sellerName;

    private String buyerTaxNo;

    private String buyerName;

    /** MANUAL 手工登记 / XML 数电票文件导入 / EXCEL 发票清单导入 */
    private InvoiceSource source;

    /** VALID / VOID */
    private InvoiceStatus status;

    /** 进项认证期间 yyyyMM */
    private Integer certifyPeriod;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String partnerName;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private List<FinInvoiceItem> items;
    /** 发票明细行（导入的数电票） */
    @TableField(exist = false)
    private List<FinInvoiceLine> lines;
}
