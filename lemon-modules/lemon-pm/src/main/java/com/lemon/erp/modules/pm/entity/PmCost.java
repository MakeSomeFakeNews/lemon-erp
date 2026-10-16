package com.lemon.erp.modules.pm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目费用
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_cost")
public class PmCost extends BaseEntity {

    /** 项目 */
    private Long projectId;

    /** 成本类型(字典 pm_cost_type)，人工成本来自工时，不在此录入 */
    private String costType;

    /** 金额（不含税） */
    private BigDecimal amount;

    /** 发生日期 */
    private LocalDate costDate;

    /** 说明 */
    private String description;

    /** 来源 MANUAL手工 ISSUE项目领料 */
    private String source;

    /** APPROVED 已生效 / APPROVING 审批中 / REJECTED 已拒绝；只有手工录入的费用会审批 */
    private String status;

    private String flowInstanceId;

    /** 项目月结凭证（已入总账） */
    private Long voucherId;

    /** 来源单号（领料生成的出库单号） */
    private String sourceNo;

    /** 录入人 */
    private Long createBy;

    @TableField(exist = false)
    private String projectName;
    @TableField(exist = false)
    private String createByName;
}
