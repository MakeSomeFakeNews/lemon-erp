package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 供应商对账单（单号、期间、状态等公共字段见 {@link FinStatementBase}）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_supplier_statement")
public class FinSupplierStatement extends FinStatementBase {

    private Long supplierId;

    /** 对账金额（应付单价税合计，退货、索赔为负） */
    private BigDecimal amount;

    private Integer billCount;

    private String supplierRemark;

    private String invoiceNo;

    private BigDecimal invoiceAmount;

    private LocalDate invoiceDate;

    @TableField(exist = false)
    private String supplierCode;
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private List<FinSupplierStatementLine> lines;
}
