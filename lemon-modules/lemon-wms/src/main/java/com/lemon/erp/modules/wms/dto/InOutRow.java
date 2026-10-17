package com.lemon.erp.modules.wms.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收发存汇总的一行：期初 + 各类入库 - 各类出库 = 期末（数量，按基本单位）
 */
@Data
public class InOutRow {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private String category;
    private BigDecimal openQty;
    /** 采购入库 */
    private BigDecimal purchaseIn;
    /** 生产入库（完工） */
    private BigDecimal productionIn;
    /** 销售退货入库 */
    private BigDecimal salesReturn;
    /** 其他入库（盘盈、期初、组装拆卸、调入等） */
    private BigDecimal otherIn;
    /** 销售出库 */
    private BigDecimal salesOut;
    /** 生产领料（扣除退料） */
    private BigDecimal productionOut;
    /** 采购退货 */
    private BigDecimal purchaseReturn;
    /** 其他出库（盘亏、项目、委外、报废、调出等） */
    private BigDecimal otherOut;
    private BigDecimal closeQty;
}
