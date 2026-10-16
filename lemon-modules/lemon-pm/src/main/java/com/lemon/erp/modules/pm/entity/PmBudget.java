package com.lemon.erp.modules.pm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 项目成本预算
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_budget")
public class PmBudget extends BaseEntity {

    /** 项目 */
    private Long projectId;

    /** 成本类型(字典 pm_cost_type) */
    private String costType;

    /** 预算金额 */
    private BigDecimal amount;

    /** 实际发生 */
    @TableField(exist = false)
    private BigDecimal actual;
}
