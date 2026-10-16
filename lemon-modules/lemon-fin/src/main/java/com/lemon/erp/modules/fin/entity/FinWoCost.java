package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工单成本：期初在产品 + 本期材料、人工、制造费用，按完工比例结转完工产品成本
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_wo_cost")
public class FinWoCost extends BaseEntity {

    private Integer period;

    private Long woId;

    private BigDecimal wipOpen;

    private BigDecimal materialCost;

    private BigDecimal laborCost;

    private BigDecimal overheadCost;

    private BigDecimal hours;

    private BigDecimal finishedQty;

    private BigDecimal finishedAmount;

    private BigDecimal unitCost;

    private BigDecimal wipClose;

    /** 关闭工单转出的在产品成本（生产损失） */
    private BigDecimal lossAmount;

    @TableField(exist = false)
    private String woNo;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String unit;
}
