package com.lemon.erp.modules.bi.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义报表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bi_report")
public class BiReport extends BaseEntity {

    /** 报表名称 */
    private String name;

    /** 数据集（分析主题） */
    private String dataset;

    /** 分析配置 JSON：行维度、列维度、指标、筛选、图表类型 */
    private String config;

    /** 是否共享给所有人 1是 0仅自己 */
    private Integer shared;

    /** 说明 */
    private String remark;

    /** 创建人 */
    private Long createBy;

    @TableField(exist = false)
    private String createByName;
    /** 是否本人创建 */
    @TableField(exist = false)
    private Boolean mine;
}
