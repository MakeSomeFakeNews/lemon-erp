package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.modules.wms.model.StockAlert;
import com.lemon.erp.modules.wms.model.StockAlertMode;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 按物料汇总的库存
 */
@Data
public class StockSummaryView {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private String category;
    /** 主图 URL */
    private String imageUrl;
    /** 库存预警方式 */
    private StockAlertMode alertMode;
    /**
     * 安全库存（库存下限）：按总库存取物料的设置（针对所有仓库的合计）；分仓库取所选仓库的设置，不选仓库时为各仓库之和
     */
    private BigDecimal safetyStock;
    /** 库存上限，口径同安全库存 */
    private BigDecimal maxStock;
    /** 预警：低于下限 / 高于上限，正常为空（按总库存比较所有仓库的合计，分仓库按仓库判断） */
    private StockAlert stockAlert;
    /** 分仓库预警时低于下限、高于上限的仓库数 */
    private Integer lowWarehouses;
    private Integer highWarehouses;
    private BigDecimal qty;

    /** 冻结数量（待检、不合格），不能出库 */
    private BigDecimal frozenQty;

    /** 均价（实时移动加权平均，按物料），需要「查看成本金额」权限；供应商寄售仓为空 */
    private BigDecimal unitCost;

    /** 库存金额 = 数量 × 均价，需要「查看成本金额」权限 */
    private BigDecimal amount;
}
