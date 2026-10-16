package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 物料标准成本：采购件手工维护单价（材料成本），自制件按 BOM 和工艺路线卷算
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_std_cost")
public class FinStdCost extends BaseEntity {

    private Long versionId;

    private Long materialId;

    /** 1 自制件 0 采购件 */
    private Integer make;

    private Long bomId;

    private BigDecimal materialCost;

    private BigDecimal laborCost;

    private BigDecimal overheadCost;

    private BigDecimal unitCost;

    private BigDecimal stdHours;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private String bomVersion;
}
