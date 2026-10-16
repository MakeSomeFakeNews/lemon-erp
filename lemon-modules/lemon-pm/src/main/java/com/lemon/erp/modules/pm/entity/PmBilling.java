package com.lemon.erp.modules.pm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目收款计划
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_billing")
public class PmBilling extends BaseEntity {

    /** 项目 */
    private Long projectId;

    /** 款项名称，如 预付款、验收款 */
    private String name;

    /** 关联里程碑 */
    private Long taskId;

    /** 计划开票日期 */
    private LocalDate planDate;

    /** 金额（含税） */
    private BigDecimal amount;

    /** 状态(字典 pm_billing_status) PLANNED待开票 BILLED已开票 */
    private String status;

    /** 生成的应收单 */
    private Long billId;

    /** 应收单号 */
    private String billNo;

    /** 开票日期 */
    private LocalDate billDate;

    /** 备注 */
    private String remark;

    @TableField(exist = false)
    private String taskName;
    /** 应收单已核销金额 */
    @TableField(exist = false)
    private BigDecimal settledAmount;
}
