package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 客户执行价：客户 × 物料的含税单价（基本单位、本位币），开单默认价最优先
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_customer_price")
public class ScmCustomerPrice extends BaseEntity {

    private Long customerId;

    private Long materialId;

    private BigDecimal price;

    /** 税率（%），为空时开单不改税率 */
    private BigDecimal taxRate;

    private String remark;

    /** 导出时填写 */
    @TableField(exist = false)
    private String customerCode;

    /** 导出时填写 */
    @TableField(exist = false)
    private String customerName;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String unit;
}
