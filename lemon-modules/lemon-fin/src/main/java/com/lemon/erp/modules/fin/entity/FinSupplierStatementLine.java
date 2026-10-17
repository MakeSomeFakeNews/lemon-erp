package com.lemon.erp.modules.fin.entity;

import com.lemon.erp.modules.fin.model.BillSource;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 对账单明细：生成时的应付单快照
 */
@Data
@TableName("fin_supplier_statement_line")
public class FinSupplierStatementLine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long statementId;

    private Long billId;

    private String billNo;

    private BillSource sourceType;

    private String sourceNo;

    private LocalDate billDate;

    private BigDecimal amount;

    private BigDecimal taxAmount;
}
