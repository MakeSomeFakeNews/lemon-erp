package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 预算：按年度、部门、科目编制各月预算，执行数取已记账凭证
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_budget")
public class FinBudget extends BaseEntity {

    private Integer budgetYear;

    /** 部门，0 为全公司 */
    private Long deptId;

    private Long accountId;

    private BigDecimal m01;

    private BigDecimal m02;

    private BigDecimal m03;

    private BigDecimal m04;

    private BigDecimal m05;

    private BigDecimal m06;

    private BigDecimal m07;

    private BigDecimal m08;

    private BigDecimal m09;

    private BigDecimal m10;

    private BigDecimal m11;

    private BigDecimal m12;

    private BigDecimal total;

    /** 预算控制：NONE / WARN / BLOCK（费用报销） */
    private String controlMode;

    private String remark;

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private String accountCode;

    @TableField(exist = false)
    private String accountName;

    @TableField(exist = false)
    private String accountCategory;
}
