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
 * @param taxRate        直接开单：税率（%），为空时取表头发票类型的税率，没有时取业务参数「默认税率」
 * @param warehouseId    仅销售出库（业务参数 scm.salesOut.lineWarehouse 开启时）：本行出库的仓库，为空取表头仓库
 * @param gift           仅直接开单的销售出库：买赠促销的赠品行（单价为 0，不计应收），同一赠品可拆成多行（不同库位、批次、仓库），
 *                       各赠品的数量合计须与促销规则计算的一致；请求中没有赠品行时按先到期先出自动补上
 * @param unitName       直接开单：录入时使用的辅助单位（仅用于显示，数量、单价仍按基本单位）
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
        Long warehouseId,
        Boolean gift,
        @Size(max = 16, message = "{scm.valid.unitCannotExceedCharacters}") String unitName) {

    /** 引用订单明细 */
    public TradeDocItemRequest(Long orderItemId, Long locationId, String batchNo, LocalDate productionDate, LocalDate expiryDate,
                               BigDecimal qty, String serialNos, String remark) {
        this(orderItemId, locationId, batchNo, productionDate, expiryDate, qty, serialNos, remark, null, null, null, null, null, null);
    }

    /** 直接开单的普通行（不含赠品、辅助单位） */
    public TradeDocItemRequest(Long orderItemId, Long locationId, String batchNo, LocalDate productionDate, LocalDate expiryDate,
                               BigDecimal qty, String serialNos, String remark, Long materialId, BigDecimal price, BigDecimal taxRate,
                               Long warehouseId) {
        this(orderItemId, locationId, batchNo, productionDate, expiryDate, qty, serialNos, remark, materialId, price, taxRate, warehouseId,
                null, null);
    }

    /** 是否赠品行 */
    public boolean isGift() {
        return Boolean.TRUE.equals(gift);
    }
}
