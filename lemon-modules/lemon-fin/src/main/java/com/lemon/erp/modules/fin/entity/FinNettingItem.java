package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.BillKind;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收应付对冲的核销明细：一张应收单或应付单的核销金额（与单据金额同号）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_netting_item")
public class FinNettingItem extends BaseEntity {

    private Long nettingId;

    private Long billId;

    /** AR 应收 / AP 应付 */
    private BillKind kind;

    private BigDecimal amount;

    @TableField(exist = false)
    private String billNo;
    @TableField(exist = false)
    private LocalDate billDate;
    @TableField(exist = false)
    private String sourceType;
    @TableField(exist = false)
    private String sourceNo;
    @TableField(exist = false)
    private BigDecimal billAmount;
}
