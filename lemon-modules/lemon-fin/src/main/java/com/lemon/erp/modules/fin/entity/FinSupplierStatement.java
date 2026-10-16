package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.StatementStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商对账单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_supplier_statement")
public class FinSupplierStatement extends BaseEntity {

    private String statementNo;

    private Long supplierId;

    private LocalDate beginDate;

    private LocalDate endDate;

    /** 对账金额（应付单价税合计，退货、索赔为负） */
    private BigDecimal amount;

    private Integer billCount;

    private StatementStatus status;

    private String supplierRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;

    private String invoiceNo;

    private BigDecimal invoiceAmount;

    private LocalDate invoiceDate;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String supplierCode;
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private List<FinSupplierStatementLine> lines;
}
