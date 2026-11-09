package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 物料的实时库存成本（移动加权平均，按物料不分仓库，不含供应商寄售仓的库存）。只能通过 StockService 修改
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_material_cost")
public class WmsMaterialCost extends BaseEntity {

    private Long materialId;

    /** 计价数量 */
    private BigDecimal qty;

    /** 库存金额 */
    private BigDecimal amount;

    /** 移动加权平均单价 */
    private BigDecimal unitCost;
}
