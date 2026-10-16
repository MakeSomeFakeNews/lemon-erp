package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 存货成本：月末一次加权平均单价 = (期初金额 + 采购金额 + 生产成本) / (期初数量 + 采购数量 + 生产数量)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_material_cost")
public class FinMaterialCost extends BaseEntity {

    private Integer period;

    private Long materialId;

    private BigDecimal openQty;

    private BigDecimal openAmount;

    private BigDecimal purchaseQty;

    private BigDecimal purchaseAmount;

    private BigDecimal makeQty;

    private BigDecimal makeAmount;

    private BigDecimal otherInQty;

    private BigDecimal otherInAmount;

    private BigDecimal issueQty;

    private BigDecimal issueAmount;

    private BigDecimal salesQty;

    private BigDecimal salesAmount;

    private BigDecimal otherOutQty;

    private BigDecimal otherOutAmount;

    private BigDecimal closeQty;

    private BigDecimal closeAmount;

    private BigDecimal unitCost;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String unit;

    @TableField(exist = false)
    private String category;
}
