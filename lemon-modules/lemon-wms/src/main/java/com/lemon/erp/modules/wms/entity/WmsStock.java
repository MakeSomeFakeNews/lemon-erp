package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 库存余额：仓库 + 库位 + 物料 + 批次 唯一。只能通过 StockService 修改
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_stock")
public class WmsStock extends BaseEntity {

    private Long warehouseId;

    /** 0 为未指定库位 */
    private Long locationId;

    private Long materialId;

    /** 未启用批次管理为空串 */
    private String batchNo;

    private BigDecimal qty;

    /** 冻结数量（待检、不合格），不能出库 */
    private BigDecimal frozenQty;
}
