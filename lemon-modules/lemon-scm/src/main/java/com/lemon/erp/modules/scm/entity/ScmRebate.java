package com.lemon.erp.modules.scm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.RebateCalcMode;
import com.lemon.erp.modules.scm.model.RebateKind;
import com.lemon.erp.modules.scm.model.RebateSettle;
import com.lemon.erp.modules.scm.model.RebateStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 返利协议：销售返利（客户）/ 采购返利（供应商）。阶梯比例：期间金额（出库 - 退货或入库 - 退货，含税）达到的最高一档返利率 × 全部金额；
 * 按项目填写金额：返利 = 各项目金额合计
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_rebate")
public class ScmRebate extends BaseEntity {

    private String rebateNo;

    /** SALES 销售返利 / PURCHASE 采购返利 */
    private RebateKind kind;

    /** TIER 阶梯比例 / ITEM 按项目填写金额 */
    private RebateCalcMode calcMode;

    /** 客户（销售返利） */
    private Long customerId;

    /** 供应商（采购返利） */
    private Long supplierId;

    private LocalDate beginDate;

    private LocalDate endDate;

    private RebateSettle settleMode;

    private RebateStatus status;

    /** 结算：期间销售额 / 采购额（按项目填写时仅供参考） */
    private BigDecimal salesAmount;

    /** 结算：返利率（按项目填写时为空） */
    private BigDecimal rate;

    private BigDecimal rebateAmount;

    private LocalDate settleDate;

    private Long settleBy;

    /** 红字应收单（销售返利）/ 红字应付单（采购返利） */
    private Long billId;

    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String billNo;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private List<ScmRebateTier> tiers;
    @TableField(exist = false)
    private List<ScmRebateItem> items;
}
