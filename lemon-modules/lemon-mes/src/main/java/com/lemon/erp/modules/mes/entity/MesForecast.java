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

    @NotNull(message = "{mes.valid.selectMaterial}")
    private Long materialId;

    /** 月份 yyyyMM */
    @NotNull(message = "{mes.valid.selectMonth}")
    private Integer period;

    @NotNull(message = "{mes.valid.enterForecastQuantity}")
    @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.forecastQuantityMustGreater}")
    @Digits(integer = 14, fraction = 4, message = "{mes.valid.quantityAllowsMostDecimalPlaces}")
    private BigDecimal qty;

    @Size(max = 255, message = "{mes.valid.remarkCannotExceedCharacters}")
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
