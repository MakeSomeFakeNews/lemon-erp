package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.modules.mes.model.ItemMethod;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 检验方案项目
 */
@Data
@TableName("qms_inspection_plan_item")
public class QmsInspectionPlanItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Integer seq;

    private String name;

    private ItemMethod method;

    private String standard;

    private BigDecimal target;

    private BigDecimal lowerLimit;

    private BigDecimal upperLimit;

    private String unit;

    /** 关键项目：不合格即判整批不合格 */
    private Integer critical;
}
