package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 销售提成表明细：每个业务员一行，detail 为按依据、分类的计算明细（JSON）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_commission_line")
public class FinCommissionLine extends BaseEntity {

    private Long commissionId;

    private Long salesmanId;

    private BigDecimal salesAmount;

    private BigDecimal profitAmount;

    private BigDecimal receiptAmount;

    private BigDecimal commissionAmount;

    private BigDecimal adjustAmount;

    private BigDecimal finalAmount;

    private String detail;

    private String remark;

    @TableField(exist = false)
    private String salesmanName;
}
