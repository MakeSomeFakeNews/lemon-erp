package com.lemon.erp.modules.scm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.RebateSettle;
import com.lemon.erp.modules.scm.model.RebateStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售返利协议：期间销售额（出库 - 退货，含税）达到的最高一档返利率 × 全部销售额
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_rebate")
public class ScmRebate extends BaseEntity {

    private String rebateNo;

    private Long customerId;

    private LocalDate beginDate;

    private LocalDate endDate;

    private RebateSettle settleMode;

    private RebateStatus status;

    private BigDecimal salesAmount;

    private BigDecimal rate;

    private BigDecimal rebateAmount;

    private LocalDate settleDate;

    private Long settleBy;

    /** 红字应收单 */
    private Long billId;

    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String billNo;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private List<ScmRebateTier> tiers;
}
