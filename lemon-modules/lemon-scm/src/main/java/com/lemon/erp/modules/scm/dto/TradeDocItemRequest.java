package com.lemon.erp.modules.scm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 入库 / 出库 / 退货明细。引用订单时对应一行订单明细，同一订单明细可以拆成多行（不同库位、批次、仓库）；
 * 直接开单时填写物料、含税单价、税率（每行生成一行隐式订单明细）
 *
 * @param orderItemId    订单明细，引用订单时必填
 * @param productionDate 生产日期，仅采购入库
 * @param expiryDate     有效期至，仅采购入库
 * @param serialNos      序列号，逗号或换行分隔
 * @param materialId     直接开单：物料
 * @param price          直接开单：含税单价
 * @param taxRate        直接开单：税率（%），为空按默认税率
 * @param warehouseId    仅销售出库（业务参数 scm.salesOut.lineWarehouse 开启时）：本行出库的仓库，为空取表头仓库
 */
public record TradeDocItemRequest(
        Long orderItemId,
        Long locationId,
        @Size(max = 64, message = "{scm.valid.batchNoCannotExceedCharacters}") String batchNo,
        LocalDate productionDate,
        LocalDate expiryDate,
        @NotNull(message = "{scm.valid.enterQuantity}")
        @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityMustGreater}")
        @Digits(integer = 14, fraction = 4, message = "{scm.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
        String serialNos,
        @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark,
        Long materialId,
        @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}")
        @Digits(integer = 14, fraction = 4, message = "{scm.valid.unitPriceAllowsMostDecimalPlaces}") BigDecimal price,
        @DecimalMin(value = "0", message = "{scm.valid.taxRateCannotNegative}")
        @DecimalMax(value = "100", message = "{scm.valid.taxRateCannotExceed}") BigDecimal taxRate,
        Long warehouseId) {

    /** 引用订单明细 */
    public TradeDocItemRequest(Long orderItemId, Long locationId, String batchNo, LocalDate productionDate, LocalDate expiryDate,
                               BigDecimal qty, String serialNos, String remark) {
        this(orderItemId, locationId, batchNo, productionDate, expiryDate, qty, serialNos, remark, null, null, null, null);
    }
}
