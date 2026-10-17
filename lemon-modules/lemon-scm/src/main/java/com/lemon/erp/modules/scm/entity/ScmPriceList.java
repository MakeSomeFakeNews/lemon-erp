package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售价目表：物料 + 客户等级（为空适用全部客户）+ 起订数量（阶梯）+ 有效期。
 * 销售订单、报价单选择物料后按 客户等级精确匹配优先、数量达到的最高一档、在有效期内 取价
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_price_list")
public class ScmPriceList extends BaseEntity {

    @NotNull(message = "{scm.valid.selectMaterial}")
    private Long materialId;

    /** 客户等级（字典 scm_customer_level），为空适用全部客户 */
    private String customerLevel;

    /** 客户专属价：指定客户时只适用该客户，优先于客户等级 */
    private Long customerId;

    /** 起订数量（阶梯），默认 0 */
    @DecimalMin(value = "0", message = "{scm.valid.minimumQuantityCannotLess}")
    private BigDecimal minQty;

    @NotNull(message = "{scm.valid.enterUnitPrice}")
    @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}")
    @Digits(integer = 14, fraction = 4, message = "{scm.valid.unitPriceAllowsMostDecimalPlaces}")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "{scm.valid.taxRateCannotNegative}")
    @DecimalMax(value = "100", message = "{scm.valid.taxRateCannotExceed}")
    private BigDecimal taxRate;

    private LocalDate validFrom;

    private LocalDate validTo;

    @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}")
    private String remark;

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
