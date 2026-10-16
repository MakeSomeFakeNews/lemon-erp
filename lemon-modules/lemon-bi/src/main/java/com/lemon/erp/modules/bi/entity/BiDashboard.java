package com.lemon.erp.modules.bi.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义仪表盘
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bi_dashboard")
public class BiDashboard extends BaseEntity {

    /** 仪表盘名称 */
    private String name;

    /** 组件 JSON：[{reportId, span, chart}] */
    private String widgets;

    /** 是否共享给所有人 */
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
