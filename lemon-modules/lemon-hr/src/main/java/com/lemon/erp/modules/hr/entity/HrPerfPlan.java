package com.lemon.erp.modules.hr.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

/**
 * 绩效考核方案
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_perf_plan")
public class HrPerfPlan extends BaseEntity {

    /** 考核名称，如 2026年第三季度绩效考核 */
    private String name;

    /** 周期 MONTH月度 QUARTER季度 YEAR年度 */
    private String periodType;

    /** 考核开始日期 */
    private LocalDate startDate;

    /** 考核结束日期 */
    private LocalDate endDate;

    /** 绩效奖金计入的工资月份 yyyyMM，为空不发放 */
    private Integer payPeriod;

    /** 状态(字典 hr_perf_status) DRAFT草稿 SCORING考核中 FINISHED已归档 */
    private String status;

    /** 说明 */
    private String remark;

    /** 创建人 */
    private Long createBy;

    @TableField(exist = false)
    private List<HrPerfIndicator> indicators;
    /** 考核人数 */
    @TableField(exist = false)
    private Integer total;
    /** 已完成人数 */
    @TableField(exist = false)
    private Integer doneCount;
    @TableField(exist = false)
    private String createByName;
}
