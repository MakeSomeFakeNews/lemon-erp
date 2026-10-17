package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.PriceControlMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 价格控制：物料的最低销售价、最高采购价（含税，基本单位，本位币）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_price_control")
public class ScmPriceControl extends BaseEntity {

    @NotNull(message = "请选择物料")
    private Long materialId;

    @DecimalMin(value = "0", message = "最低销售价不能小于0")
    @Digits(integer = 14, fraction = 4, message = "单价最多4位小数")
    private BigDecimal minSalePrice;

    @DecimalMin(value = "0", message = "最高采购价不能小于0")
    @Digits(integer = 14, fraction = 4, message = "单价最多4位小数")
    private BigDecimal maxPurchasePrice;

    private PriceControlMode controlMode;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
}
