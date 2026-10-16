package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_order_change_item")
public class ScmOrderChangeItem extends BaseEntity {

    private Long changeId;

    /** ADD / MODIFY / REMOVE */
    private String action;

    private Long orderItemId;

    private Long materialId;

    private BigDecimal oldQty;

    private BigDecimal newQty;

    private BigDecimal oldPrice;

    private BigDecimal newPrice;

    private BigDecimal taxRate;

    private LocalDate oldDeliveryDate;

    private LocalDate newDeliveryDate;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String unit;
}
