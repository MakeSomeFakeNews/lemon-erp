package com.lemon.erp.modules.pm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 项目月结：按完工百分比确认的收入和结转的成本（每次月结的增量）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_revenue")
public class PmRevenue extends BaseEntity {

    private Long projectId;

    /** 期间 yyyyMM */
    private Integer period;

    /** 完工进度（%） */
    private BigDecimal completionRate;

    /** 累计应确认收入（不含税） */
    private BigDecimal cumRevenue;

    /** 本次确认的收入 */
    private BigDecimal revenue;

    /** 本次结转的成本 */
    private BigDecimal cost;

    /** 本次计入合同履约成本的人工、费用 */
    private BigDecimal laborCost;
    private BigDecimal expenseCost;

    private Long voucherId;

    private Long createBy;

    @TableField(exist = false)
    private String voucherNo;
}
