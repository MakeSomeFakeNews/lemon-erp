package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.CommissionBasis;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 销售提成规则
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_commission_rule")
public class FinCommissionRule extends BaseEntity {

    @NotBlank(message = "{fin.valid.enterRuleName}")
    @Size(max = 64, message = "{fin.valid.ruleNameCannotExceedCharacters}")
    private String name;

    @NotNull(message = "{fin.valid.selectCommissionBasis}")
    private CommissionBasis basis;

    /** 适用业务员，为空适用全部 */
    private Long salesmanId;

    /** 适用物料分类，为空适用全部 */
    private String materialCategory;

    /** 业绩门槛（阶梯提成） */
    @DecimalMin(value = "0", message = "{fin.valid.performanceThresholdCannotLess}")
    private BigDecimal threshold;

    @NotNull(message = "{fin.valid.enterCommissionRate}")
    @DecimalMin(value = "0", message = "{fin.valid.commissionRateCannotLess}")
    @DecimalMax(value = "100", message = "{fin.valid.commissionRateCannotExceed}")
    private BigDecimal rate;

    private Integer status;

    @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}")
    private String remark;

    @TableField(exist = false)
    private String salesmanName;
}
