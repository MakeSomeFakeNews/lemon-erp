package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 销售预测：按物料、月份预计的销售数量，MRP 可选择计入（扣减同月已有的销售订单后作为需求）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_forecast")
public class MesForecast extends BaseEntity {

    @NotNull(message = "请选择物料")
    private Long materialId;

    /** 月份 yyyyMM */
    @NotNull(message = "请选择月份")
    private Integer period;

    @NotNull(message = "请填写预测数量")
    @DecimalMin(value = "0", inclusive = false, message = "预测数量必须大于0")
    @Digits(integer = 14, fraction = 4, message = "数量最多4位小数")
    private BigDecimal qty;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
}
