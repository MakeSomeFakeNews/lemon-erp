package com.lemon.erp.modules.fin.entity;

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
    private String kind;

    private String invoiceNo;

    /** SPECIAL 专票 / NORMAL 普票 */
    private String invoiceType;

    private Long partnerId;

    private LocalDate invoiceDate;

    /** 价税合计 */
    private BigDecimal amount;

    private BigDecimal taxAmount;

    /** VALID / VOID */
    private String status;

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
}
