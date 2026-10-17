package com.lemon.erp.modules.crm.entity;

import com.lemon.erp.modules.crm.model.CollectStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同回款计划
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_collection_plan")
public class CrmCollectionPlan extends BaseEntity {

    private Long contractId;

    private Integer seq;

    private LocalDate planDate;

    private BigDecimal amount;

    private String remark;

    /** 已回款（实际回款按计划日期先后分摊） */
    @TableField(exist = false)
    private BigDecimal receivedAmount;

    /** 回款状态 */
    @TableField(exist = false)
    private CollectStatus collectStatus;

    @TableField(exist = false)
    private String contractNo;
    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private Long ownerId;
}
