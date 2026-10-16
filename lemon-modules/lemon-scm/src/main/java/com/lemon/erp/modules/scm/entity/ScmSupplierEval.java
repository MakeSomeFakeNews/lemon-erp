package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 供应商评估：按质量、交期、价格、服务打分，加权得出综合得分和等级。新增使用 SupplierEvalRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_supplier_eval")
public class ScmSupplierEval extends BaseEntity {

    private Long supplierId;

    /** 考核期间，如 2026-Q3 */
    private String period;

    private BigDecimal qualityScore;

    private BigDecimal deliveryScore;

    private BigDecimal priceScore;

    private BigDecimal serviceScore;

    /** 综合得分 = 质量 40% + 交期 30% + 价格 20% + 服务 10% */
    private BigDecimal totalScore;

    /** 评定等级，字典 scm_supplier_level */
    private String grade;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String supplierCode;

    @TableField(exist = false)
    private String supplierName;

    @TableField(exist = false)
    private String createByName;
}
