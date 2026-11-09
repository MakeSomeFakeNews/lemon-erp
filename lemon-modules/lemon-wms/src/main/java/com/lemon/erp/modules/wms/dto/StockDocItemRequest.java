package com.lemon.erp.modules.wms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 出入库单明细
 *
 * @param locationId     库位；调拨为调出库位，不填为未指定库位
 * @param toLocationId   调入库位，仅调拨
 * @param batchNo        批次号，启用批次管理的物料必填
 * @param productionDate 生产日期，仅入库
 * @param expiryDate     有效期至，仅入库
 * @param serialNos      序列号，逗号或换行分隔；启用序列号管理的物料，个数须与数量一致
 * @param warehouseId    本行的仓库（入库、出库），为空取表头仓库；调拨忽略。目前只有销售出库（明细可选仓库）使用
 * @param costPrice      成本单价：其他入库、期初入库填写的入库成本（为空按当前移动加权平均价）；
 *                       业务单据生成时传入（采购入库的不含税单价、销售退货的原出库成本等）。手工出库、调拨忽略
 * @param bizPrice       业务单价（来源单据的不含税单价，本位币），只由业务单据传入；手工单据忽略
 */
public record StockDocItemRequest(
        @NotNull(message = "{wms.valid.selectMaterial}") Long materialId,
        Long locationId,
        Long toLocationId,
        @Size(max = 64, message = "{wms.valid.batchNoCannotExceedCharacters}") String batchNo,
        LocalDate productionDate,
        LocalDate expiryDate,
        @NotNull(message = "{wms.valid.enterQuantity}")
        @DecimalMin(value = "0", inclusive = false, message = "{wms.valid.quantityMustGreater}")
        @Digits(integer = 14, fraction = 4, message = "{wms.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
        String serialNos,
        @Size(max = 255, message = "{wms.valid.remarkCannotExceedCharacters}") String remark,
        Long warehouseId,
        @DecimalMin(value = "0", message = "{wms.valid.costPriceCannotNegative}")
        @Digits(integer = 12, fraction = 6, message = "{wms.valid.costPriceAllowsMostDecimalPlaces}") BigDecimal costPrice,
        BigDecimal bizPrice) {

    public StockDocItemRequest(Long materialId, Long locationId, Long toLocationId, String batchNo, LocalDate productionDate,
                               LocalDate expiryDate, BigDecimal qty, String serialNos, String remark, Long warehouseId) {
        this(materialId, locationId, toLocationId, batchNo, productionDate, expiryDate, qty, serialNos, remark, warehouseId, null, null);
    }

    /** 使用表头仓库 */
    public StockDocItemRequest(Long materialId, Long locationId, Long toLocationId, String batchNo, LocalDate productionDate,
                               LocalDate expiryDate, BigDecimal qty, String serialNos, String remark) {
        this(materialId, locationId, toLocationId, batchNo, productionDate, expiryDate, qty, serialNos, remark, null);
    }

    /** 带上成本单价、业务单价 */
    public StockDocItemRequest withCost(BigDecimal newCostPrice, BigDecimal newBizPrice) {
        return new StockDocItemRequest(materialId, locationId, toLocationId, batchNo, productionDate, expiryDate, qty, serialNos,
                remark, warehouseId, newCostPrice, newBizPrice);
    }
}
