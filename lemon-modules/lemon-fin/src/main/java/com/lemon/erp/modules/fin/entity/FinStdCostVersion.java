package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.StdCostStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 标准成本版本：期间 P 的成本差异分析使用生效期间不晚于 P 的最近一个已发布版本
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_std_cost_version")
public class FinStdCostVersion extends BaseEntity {

    private String name;

    private Integer effectivePeriod;

    private StdCostStatus status;

    /** 最近卷算时间，修改采购件单价后清空（须重新卷算才能发布） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rollupTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime releaseTime;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String createByName;
    /** 物料数、自制件数、单价为 0 的采购件数 */
    @TableField(exist = false)
    private Integer itemCount;
    @TableField(exist = false)
    private Integer makeCount;
    @TableField(exist = false)
    private Integer zeroCount;
}
