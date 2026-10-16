package com.lemon.erp.modules.crm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售合同：草稿 → 审批（发布了「销售合同审批」时）→ 执行中 → 已完成 / 已终止；回款按关联销售订单的应收核销金额计算
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_contract")
public class CrmContract extends BaseEntity {

    private String contractNo;

    private String name;

    /** 客户（scm_customer） */
    private Long customerId;

    private Long opportunityId;

    private Long quotationId;

    private Long orderId;

    private LocalDate signDate;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal amount;

    /** DRAFT / APPROVING / ACTIVE / COMPLETED / TERMINATED */
    private String status;

    private Long ownerId;

    private Long deptId;

    private String flowInstanceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    private String terminateReason;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String ownerName;
    @TableField(exist = false)
    private String opportunityName;
    @TableField(exist = false)
    private String quoteNo;
    @TableField(exist = false)
    private String orderNo;
    /** 已回款（关联订单的应收单核销金额） */
    @TableField(exist = false)
    private BigDecimal receivedAmount;
    @TableField(exist = false)
    private List<CrmCollectionPlan> plans;
}
