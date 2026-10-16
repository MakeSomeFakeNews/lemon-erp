package com.lemon.erp.modules.crm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商机产品明细
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_opportunity_item")
public class CrmOpportunityItem extends BaseEntity {

    /** 商机 */
    private Long opportunityId;

    /** 产品 */
    private Long materialId;

    /** 数量 */
    private BigDecimal qty;

    /** 含税单价 */
    private BigDecimal price;

    /** 税率（%） */
    private BigDecimal taxRate;

    /** 金额 = 数量 × 单价 */
    private BigDecimal amount;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String unit;
}
