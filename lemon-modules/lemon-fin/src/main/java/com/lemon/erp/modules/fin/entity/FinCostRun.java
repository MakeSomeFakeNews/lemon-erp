package com.lemon.erp.modules.fin.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.fin.model.VoucherStatus;
import com.lemon.erp.modules.fin.model.CostRunKind;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 成本核算记录：每个期间一条，可以重新核算（成本结转凭证未记账时）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_cost_run")
public class FinCostRun extends BaseEntity {

    private Integer period;

    private Long voucherId;

    /** RUN 核算 / OPENING 存货期初 */
    private CostRunKind kind;

    /** 本期转入主营业务成本的制造费用差异 */
    private BigDecimal overheadVariance;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String voucherNo;

    @TableField(exist = false)
    private VoucherStatus voucherStatus;

    @TableField(exist = false)
    private String createByName;

    /** 销售成本合计 */
    @TableField(exist = false)
    private BigDecimal salesCost;

    /** 完工产品成本合计 */
    @TableField(exist = false)
    private BigDecimal finishedCost;

    /** 期末在产品合计 */
    @TableField(exist = false)
    private BigDecimal wipCost;
}
