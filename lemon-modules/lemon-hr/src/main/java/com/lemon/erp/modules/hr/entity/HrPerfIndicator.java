package com.lemon.erp.modules.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 考核指标
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_perf_indicator")
public class HrPerfIndicator extends BaseEntity {

    /** 考核方案 */
    private Long planId;

    /** 指标名称 */
    private String name;

    /** 权重（%），合计 100 */
    private BigDecimal weight;

    /** 评分标准 */
    private String description;

    /** 排序 */
    private Integer sort;
}
