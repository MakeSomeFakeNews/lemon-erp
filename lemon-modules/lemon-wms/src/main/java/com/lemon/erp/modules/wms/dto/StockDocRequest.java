package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.modules.wms.model.StockDocType;
import com.lemon.erp.modules.wms.model.TransferFeeSettle;
import com.lemon.erp.modules.wms.model.TransferPricing;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改出入库单（入库、出库、调拨）
 *
 * @param bizType       入库类型（字典 wms_in_type）/ 出库类型（字典 wms_out_type），调拨不填
 * @param warehouseId   仓库；调拨为调出仓库
 * @param toWarehouseId 调入仓库，仅调拨
 * @param ioCategory    收发类别：其他入库为字典 wms_in_category，其他出库、报废出库为字典 wms_out_category，可不填；其他类型忽略
 * @param partnerId     往来单位（供应商 / 客户），业务单据生成时传入，记录在单据和库存流水上；手工单据忽略
 * @param partnerName   往来单位名称快照
 * @param transferPricing 调拨价格（仅调拨）：同价调拨（默认）或差价调拨的依据，见 {@link TransferPricing}
 * @param priceLevel      依据为价格列时的价格列（0 零售价，1-8 批发价）
 * @param priceFactor     差价调拨的价格系数，默认 1
 * @param fee             调拨费用（配送费，仅调拨），不填为没有
 * @param feeSettle       调拨费用付给承运商的方式：现付（默认，需付款账户）/ 月结（需承运商）
 * @param feeAccountId    现付的付款账户
 * @param feeCarrierId    承运商（供应商），月结必填，现付可选
 */
public record StockDocRequest(
        String bizType,
        @NotNull(message = "{wms.valid.selectWarehouse}") Long warehouseId,
        Long toWarehouseId,
        @NotNull(message = "{wms.valid.selectBusinessDate}") LocalDate bizDate,
        @Size(max = 255, message = "{wms.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{wms.valid.addLineItems}") @Size(max = 500, message = "{wms.valid.lineItemsCannotExceed2}") List<@Valid @NotNull StockDocItemRequest> items,
        Boolean inTransit,
        String ioCategory,
        Long partnerId,
        String partnerName,
        TransferPricing transferPricing,
        Integer priceLevel,
        @DecimalMin(value = "0", inclusive = false, message = "{wms.valid.priceFactorMustGreater}")
        @Digits(integer = 6, fraction = 4, message = "{wms.valid.priceFactorAllowsMostDecimalPlaces}") BigDecimal priceFactor,
        @DecimalMin(value = "0", message = "{wms.valid.transferFeeCannotNegative}")
        @Digits(integer = 14, fraction = 2, message = "{wms.valid.transferFeeAllowsMostDecimalPlaces}") BigDecimal fee,
        TransferFeeSettle feeSettle,
        Long feeAccountId,
        Long feeCarrierId) {

    public StockDocRequest(String bizType, Long warehouseId, Long toWarehouseId, LocalDate bizDate, String remark,
                           List<StockDocItemRequest> items, Boolean inTransit, String ioCategory, Long partnerId, String partnerName) {
        this(bizType, warehouseId, toWarehouseId, bizDate, remark, items, inTransit, ioCategory, partnerId, partnerName,
                null, null, null, null, null, null, null);
    }

    public StockDocRequest(String bizType, Long warehouseId, Long toWarehouseId, LocalDate bizDate, String remark,
                           List<StockDocItemRequest> items, Boolean inTransit, String ioCategory) {
        this(bizType, warehouseId, toWarehouseId, bizDate, remark, items, inTransit, ioCategory, null, null);
    }

    /** 业务单据生成出入库单时使用（非在途） */
    public StockDocRequest(String bizType, Long warehouseId, Long toWarehouseId, LocalDate bizDate, String remark,
                           List<StockDocItemRequest> items) {
        this(bizType, warehouseId, toWarehouseId, bizDate, remark, items, null, null);
    }

    /** 业务单据生成出入库单时使用（非在途），业务类型取入库类型 / 出库类型枚举 */
    public StockDocRequest(StockDocType bizType, Long warehouseId, Long toWarehouseId, LocalDate bizDate, String remark,
                           List<StockDocItemRequest> items) {
        this(bizType.name(), warehouseId, toWarehouseId, bizDate, remark, items, null, null);
    }

    /** 带上往来单位（记录在单据和库存流水上） */
    public StockDocRequest withPartner(Long partnerId, String partnerName) {
        return new StockDocRequest(bizType, warehouseId, toWarehouseId, bizDate, remark, items, inTransit, ioCategory, partnerId, partnerName,
                transferPricing, priceLevel, priceFactor, fee, feeSettle, feeAccountId, feeCarrierId);
    }

    /** 替换明细（如带上成本单价） */
    public StockDocRequest withItems(List<StockDocItemRequest> newItems) {
        return new StockDocRequest(bizType, warehouseId, toWarehouseId, bizDate, remark, newItems, inTransit, ioCategory, partnerId, partnerName,
                transferPricing, priceLevel, priceFactor, fee, feeSettle, feeAccountId, feeCarrierId);
    }
}
