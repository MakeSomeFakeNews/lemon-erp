package com.lemon.erp.modules.pm.entity;

import com.lemon.erp.modules.pm.model.TaskStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 项目任务（WBS）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_task")
public class PmTask extends BaseEntity {

    /** 项目 */
    private Long projectId;

    /** 上级任务，0 为顶级 */
    private Long parentId;

    /** 任务名称 */
    private String name;

    /** 负责人 */
    private Long assigneeId;

    /** 计划开始 */
    private LocalDate planStart;

    /** 计划完成 */
    private LocalDate planEnd;

    /** 计划工时，上级任务为下级合计 */
    private BigDecimal planHours;

    /** 实际工时（已审批的工时） */
    private BigDecimal actualHours;

    /** 进度（%），上级任务按下级计划工时加权 */
    private Integer progress;

    /** 状态(字典 pm_task_status) TODO未开始 DOING进行中 DONE已完成 */
    private TaskStatus status;

    /** 是否里程碑 */
    private Integer milestone;

    /** 前置任务（完成后才能开始） */
    private Long predecessorId;

    /** 实际完成日期 */
    private LocalDate actualEnd;

    /** 排序 */
    private Integer sort;

    /** 说明 */
    private String description;

    @TableField(exist = false)
    private String assigneeName;
    @TableField(exist = false)
    private String predecessorName;
    @TableField(exist = false)
    private String projectName;
    /** 未完成且超过计划完成日期 */
    @TableField(exist = false)
    private Boolean overdue;
    @TableField(exist = false)
    private List<PmTask> children;
}
