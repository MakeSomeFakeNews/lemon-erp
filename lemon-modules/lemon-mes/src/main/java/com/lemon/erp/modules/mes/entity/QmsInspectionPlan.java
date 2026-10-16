package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.mes.model.InspectionLevel;
import com.lemon.erp.modules.mes.model.QcType;
import com.lemon.erp.modules.mes.model.SamplingMode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 检验方案：按物料（优先）或物料分类定义检验项目与抽样方案
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qms_inspection_plan")
public class QmsInspectionPlan extends BaseEntity {

    private String name;

    private QcType inspType;

    /** 适用物料，与物料分类二选一 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long materialId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String materialCategory;

    private SamplingMode samplingMode;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal fixedQty;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer acceptQty;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal aql;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private InspectionLevel inspectionLevel;

    private Integer status;

    private String remark;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private Integer itemCount;
    @TableField(exist = false)
    private List<QmsInspectionPlanItem> items;
}
