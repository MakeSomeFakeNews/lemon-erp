package com.lemon.erp.modules.hr.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 指标得分
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_perf_score")
public class HrPerfScore extends BaseEntity {

    /** 考核表 */
    private Long appraisalId;

    /** 指标 */
    private Long indicatorId;

    /** 自评分（0-100） */
    private BigDecimal selfScore;

    /** 评分（0-100） */
    private BigDecimal score;

    @TableField(exist = false)
    private String indicatorName;
    @TableField(exist = false)
    private BigDecimal weight;
    @TableField(exist = false)
    private String description;
}
