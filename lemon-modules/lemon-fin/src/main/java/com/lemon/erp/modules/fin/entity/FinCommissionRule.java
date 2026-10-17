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

    @NotBlank(message = "请填写规则名称")
    @Size(max = 64, message = "规则名称不能超过64个字符")
    private String name;

    @NotNull(message = "请选择提成依据")
    private CommissionBasis basis;

    /** 适用业务员，为空适用全部 */
    private Long salesmanId;

    /** 适用物料分类，为空适用全部 */
    private String materialCategory;

    /** 业绩门槛（阶梯提成） */
    @DecimalMin(value = "0", message = "业绩门槛不能小于0")
    private BigDecimal threshold;

    @NotNull(message = "请填写提成比例")
    @DecimalMin(value = "0", message = "提成比例不能小于0")
    @DecimalMax(value = "100", message = "提成比例不能大于100%")
    private BigDecimal rate;

    private Integer status;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    @TableField(exist = false)
    private String salesmanName;
}
