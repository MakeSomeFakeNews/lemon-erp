package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 渠道：经销商订货、渠道库存销量上报、外部订单导入
 */
public final class ChannelRequests {

    private ChannelRequests() {
    }

    /** 经销商订货：单价按价目表，自动应用促销，可抵扣返利 */
    public record DealerOrder(
            LocalDate deliveryDate,
            @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark,
            @DecimalMin(value = "0", message = "{scm.valid.rebateDeductionCannotNegative}") BigDecimal rebateAmount,
            @NotEmpty(message = "{scm.valid.addMaterialsOrder}") @Size(max = 200, message = "{scm.valid.noMoreLines}") List<@Valid @NotNull Line> items) {
    }

    public record Line(
            @NotNull(message = "{scm.valid.selectMaterial}") Long materialId,
            @NotNull(message = "{scm.valid.enterQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4) BigDecimal qty) {
    }

    /** 渠道库存销量上报（同一期间重复上报覆盖） */
    public record Report(
            @Size(max = 255, message = "{scm.valid.descriptionCannotExceedCharacters}") String remark,
            @NotEmpty(message = "{scm.valid.addMaterialsReport}") @Size(max = 500, message = "{scm.valid.lineItemsCannotExceed}") List<@Valid @NotNull ReportLine> items) {
    }

    public record ReportLine(
            @NotNull(message = "{scm.valid.selectMaterial}") Long materialId,
            @NotNull(message = "{scm.valid.enterSalesQuantity}") @DecimalMin(value = "0", message = "{scm.valid.salesQuantityCannotNegative}") @Digits(integer = 14, fraction = 4) BigDecimal sellQty,
            @NotNull(message = "{scm.valid.enterStock}") @DecimalMin(value = "0", message = "{scm.valid.stockCannotNegative}") @Digits(integer = 14, fraction = 4) BigDecimal stockQty) {
    }

    /**
     * 外部订单导入的一行（统一格式，同一外部单号的多行为一张订单）
     *
     * @param price 含税单价，为空按价目表
     */
    public record ImportRow(
            @NotBlank(message = "{scm.valid.externalOrderNoRequired}") @Size(max = 64, message = "{scm.valid.externalOrderNoCannotExceedCharacters}") String externalNo,
            @NotBlank(message = "{scm.valid.customerCodeRequired}") String customerCode,
            @NotNull(message = "{scm.valid.orderDateRequired}") LocalDate orderDate,
            LocalDate deliveryDate,
            @NotBlank(message = "{scm.valid.materialCodeRequired}") String materialCode,
            @NotNull(message = "{scm.valid.quantityRequired}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{scm.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
            @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}") @Digits(integer = 14, fraction = 4, message = "{scm.valid.unitPriceAllowsMostDecimalPlaces}") BigDecimal price,
            @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark) {
    }
}
