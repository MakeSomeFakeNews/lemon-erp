package com.lemon.erp.modules.crm.entity;

import com.lemon.erp.modules.crm.model.OpportunityStage;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售机会
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_opportunity")
public class CrmOpportunity extends BaseEntity {

    /** 商机编号 */
    private String oppNo;

    /** 商机名称 */
    private String name;

    /** 客户 */
    private Long customerId;

    /** 联系人 */
    private Long contactId;

    /** 预计成交金额（含税），有产品明细时为明细合计 */
    private BigDecimal amount;

    /** 销售阶段(字典 crm_opp_stage) INITIAL初步接洽 NEEDS需求确认 PROPOSAL方案报价 NEGOTIATION商务谈判 WON赢单 LOST输单 */
    private OpportunityStage stage;

    /** 赢单概率（%），默认按阶段 */
    private Integer probability;

    /** 预计成交日期 */
    private LocalDate expectedDate;

    /** 来源(字典 crm_source) */
    private String source;

    /** 来源市场活动 */
    private Long campaignId;

    /** 来源线索 */
    private Long leadId;

    /** 负责人 */
    private Long ownerId;

    /** 负责人部门（数据权限） */
    private Long deptId;

    /** 赢单 / 输单日期 */
    private LocalDate closeDate;

    /** 输单原因 */
    private String lostReason;

    /** 生成的销售订单 */
    private Long salesOrderId;

    /** 销售订单号 */
    private String salesOrderNo;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private Long createBy;

    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String contactName;
    @TableField(exist = false)
    private String ownerName;
    @TableField(exist = false)
    private String campaignName;
    @TableField(exist = false)
    private List<CrmOpportunityItem> items;
}
