package com.lemon.erp.modules.wms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 一次库存变动，由 StockService 执行：更新库存、批次、序列号、实时库存成本并记录流水
 *
 * @param locationId     库位，0 为未指定
 * @param batchNo        批次号，未启用批次管理为空串
 * @param qty            变动数量，正数增加、负数减少
 * @param serialNos      本次增加 / 减少的序列号，数量与 |qty| 一致；非序列号物料为空
 * @param productionDate 生产日期，仅增加时用于登记新批次
 * @param expiryDate     有效期至，仅增加时用于登记新批次
 * @param unitCost       指定的成本单价（采购入库的不含税单价、其他入库填写的成本价、销售退货的原出库成本等），
 *                       为空按当前移动加权平均价；在途调拨收货为调出时的成本单价，其他调拨和盘点忽略
 * @param bizPrice       业务单价（来源单据的不含税单价，本位币），只记录在流水上
 * @param partnerId      往来单位，只记录在流水上
 * @param partnerName    往来单位名称快照（wms 不关联供应链的表，由调用方传入）
 */
public record StockChange(Long warehouseId, long locationId, Long materialId, String batchNo, BigDecimal qty,
                          List<String> serialNos, LocalDate productionDate, LocalDate expiryDate,
                          BigDecimal unitCost, BigDecimal bizPrice, Long partnerId, String partnerName) {

    public StockChange(Long warehouseId, long locationId, Long materialId, String batchNo, BigDecimal qty,
                       List<String> serialNos, LocalDate productionDate, LocalDate expiryDate) {
        this(warehouseId, locationId, materialId, batchNo, qty, serialNos, productionDate, expiryDate, null, null, null, null);
    }

    public static StockChange of(Long warehouseId, long locationId, Long materialId, String batchNo, BigDecimal qty,
                                 List<String> serialNos) {
        return new StockChange(warehouseId, locationId, materialId, batchNo, qty, serialNos, null, null);
    }

    /** 带上成本单价、业务单价和往来单位 */
    public StockChange withCost(BigDecimal unitCost, BigDecimal bizPrice, Long partnerId, String partnerName) {
        return new StockChange(warehouseId, locationId, materialId, batchNo, qty, serialNos, productionDate, expiryDate,
                unitCost, bizPrice, partnerId, partnerName);
    }

    public boolean increase() {
        return qty.signum() > 0;
    }
}
