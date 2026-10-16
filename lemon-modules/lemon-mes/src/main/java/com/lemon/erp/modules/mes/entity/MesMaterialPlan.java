package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 物料计划属性：与仓库物料一对一，由生产模块维护，MRP、工单、领料使用
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_material_plan")
public class MesMaterialPlan extends BaseEntity {

    private Long materialId;

    /** SupplyType，为空时按有无默认 BOM 判断 */
    private String supplyType;

    /** 提前期（天），为空时取 BOM / 供货价格的提前期 */
    private Integer leadDays;

    private BigDecimal minLot;

    private BigDecimal lotMultiple;

    /** 允许超领比例（%） */
    private BigDecimal overIssueRate;

    private String planner;

    /** 完工需成品检验：完工入库累计不超过已确认的成品检验合格数 */
    private Integer fqcRequired;
}
