package com.lemon.erp.modules.mes.entity;

import com.lemon.erp.modules.mes.model.PlanStatus;
import com.lemon.erp.modules.mes.model.PlanSource;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 主生产计划：某产品在某日期需要的数量，MRP 据此展开 BOM 计算物料需求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_plan")
public class MesPlan extends BaseEntity {

    private String planNo;

    private Long materialId;

    private BigDecimal qty;

    private LocalDate dueDate;

    /** MANUAL 手工 / SALES 销售订单 */
    private PlanSource source;

    private Long salesOrderId;

    private String salesOrderNo;

    /** PlanStatus */
    private PlanStatus status;

    private String remark;

    private Long deptId;

    private Long createBy;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String unit;

    @TableField(exist = false)
    private String createByName;

    /** 已转工单数量（关闭的工单按已完工数量计） */
    @TableField(exist = false)
    private BigDecimal woQty;

    @TableField(exist = false)
    private BigDecimal finishedQty;
}
