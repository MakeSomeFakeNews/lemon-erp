package com.lemon.erp.modules.pm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.pm.model.ProjectStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_project")
public class PmProject extends BaseEntity {

    /** 项目编号 */
    private String code;

    /** 项目名称 */
    private String name;

    /** 类型(字典 pm_project_type) */
    private String type;

    /** 客户（对外项目） */
    private Long customerId;

    /** 来源商机 */
    private Long opportunityId;

    /** 关联的销售订单 */
    private Long salesOrderId;

    /** 1 超预算控制 */
    private Integer budgetControl;

    /** 项目经理 */
    private Long managerId;

    /** 负责部门（数据权限） */
    private Long deptId;

    /** 优先级(字典 pm_priority) */
    private String priority;

    /** 状态(字典 pm_project_status) PLANNING规划中 ACTIVE进行中 SUSPENDED已暂停 FINISHED已完成 CLOSED已关闭 */
    private ProjectStatus status;

    /** 计划开始 */
    private LocalDate planStart;

    /** 计划完成 */
    private LocalDate planEnd;

    /** 实际开始 */
    private LocalDate actualStart;

    /** 实际完成 */
    private LocalDate actualEnd;

    /** 合同金额（含税） */
    private BigDecimal contractAmount;

    /** 税率（%） */
    private BigDecimal taxRate;

    /** 成本预算合计（各成本类型预算之和） */
    private BigDecimal budgetAmount;

    /** 完成进度（%），按任务计划工时加权 */
    private BigDecimal progress;

    /** 项目说明 */
    private String description;

    /** 立项审批流程实例ID */
    private String flowInstanceId;

    /** 创建人 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String salesOrderNo;
    @TableField(exist = false)
    private String managerName;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private Integer taskCount;
    @TableField(exist = false)
    private Integer doneTaskCount;
    /** 逾期未完成的任务数 */
    @TableField(exist = false)
    private Integer overdueTaskCount;
    @TableField(exist = false)
    private Integer memberCount;
    /** 人工成本（已审批工时） */
    @TableField(exist = false)
    private BigDecimal laborCost;
    /** 费用（材料、差旅等） */
    @TableField(exist = false)
    private BigDecimal expenseCost;
    /** 已开票 */
    @TableField(exist = false)
    private BigDecimal billedAmount;
    /** 已收款（应收单已核销金额） */
    @TableField(exist = false)
    private BigDecimal receivedAmount;
}
