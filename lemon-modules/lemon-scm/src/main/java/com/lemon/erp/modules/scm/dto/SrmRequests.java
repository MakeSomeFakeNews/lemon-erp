package com.lemon.erp.modules.scm.dto;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.scm.model.AwardTarget;
import com.lemon.erp.modules.scm.model.ContractType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购合同、招标、供应商协同的请求
 */
public final class SrmRequests {

    private SrmRequests() {
    }

    /**
     * 采购合同
     *
     * @param amountLimit 金额上限（价税合计），为空不限
     * @param paymentDays 付款天数：引用合同的采购订单生成应付单时按此计算到期日（未填按供应商账期）
     */
    public record Contract(
            @NotBlank(message = "{scm.valid.enterContractName}") @Size(max = 128, message = "{scm.valid.contractNameCannotExceedCharacters}") String name,
            @NotNull(message = "{scm.valid.selectSupplier}") Long supplierId,
            @NotNull(message = "{scm.valid.selectContractType}") ContractType contractType,
            @NotNull(message = "{scm.valid.selectSigningDate}") LocalDate signDate,
            @NotNull(message = "{scm.valid.selectEffectiveDate}") LocalDate startDate,
            @NotNull(message = "{scm.valid.selectExpiryDate}") LocalDate endDate,
            @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.amountLimitMustGreater}") @Digits(integer = 16, fraction = 2) BigDecimal amountLimit,
            @Min(value = 0, message = "{scm.valid.paymentDaysCannotLess}") @Max(value = 3650, message = "{scm.valid.paymentDaysCannotExceed}") Integer paymentDays,
            @Size(max = 255, message = "{scm.valid.paymentTermsCannotExceedCharacters}") String paymentTerms,
            @Size(max = 500, message = "{scm.valid.remarkCannotExceedCharacters2}") String remark,
            @NotEmpty(message = "{scm.valid.addContractMaterials}") @Size(max = 500, message = "{scm.valid.lineItemsCannotExceed}") List<@Valid @NotNull ContractItem> items) {
    }

    public record ContractItem(
            @NotNull(message = "{scm.valid.selectMaterial}") Long materialId,
            @NotNull(message = "{scm.valid.enterContractUnitPrice}") @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}") @Digits(integer = 14, fraction = 4) BigDecimal price,
            @DecimalMin(value = "0", message = "{scm.valid.taxRateCannotNegative}") @DecimalMax(value = "100", message = "{scm.valid.taxRateCannotExceed2}") BigDecimal taxRate,
            @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityLimitMustGreater}") @Digits(integer = 14, fraction = 4) BigDecimal qtyLimit,
            @Size(max = 255) String remark) {
    }

    public record Close(@NotBlank(message = "{scm.valid.enterCloseReason}") @Size(max = 255, message = "{scm.valid.closeReasonCannotExceedCharacters}") String reason) {
    }

    /** 招标 */
    public record Tender(
            @NotBlank(message = "{scm.valid.enterTenderTitle}") @Size(max = 128, message = "{scm.valid.titleCannotExceedCharacters}") String title,
            @NotNull(message = "{scm.valid.selectBidDeadline}") LocalDateTime deadline,
            @Size(max = 500, message = "{scm.valid.tenderDescriptionCannotExceedCharacters}") String remark,
            @NotEmpty(message = "{scm.valid.addTenderMaterials}") @Size(max = 200, message = "{scm.valid.noMoreLines}") List<@Valid @NotNull TenderItem> items,
            @NotEmpty(message = "{scm.valid.selectInvitedSuppliers}") @Size(max = 50, message = "{scm.valid.invitedSuppliersCannotExceed}") List<@NotNull Long> supplierIds) {
    }

    public record TenderItem(
            @NotNull(message = "{scm.valid.selectMaterial}") Long materialId,
            @NotNull(message = "{scm.valid.enterQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityMustGreater}") @Digits(integer = 14, fraction = 4) BigDecimal qty,
            LocalDate requiredDate,
            @Size(max = 255) String remark) {
    }

    /**
     * 投标：门户提交时忽略 supplierId（取当前账号的供应商），采购代录时必填
     */
    public record Bid(
            Long supplierId,
            @Min(value = 0, message = "{scm.valid.deliveryLeadTimeCannotLess}") @Max(value = 3650, message = "{scm.valid.deliveryLeadTimeCannotExceedDays}") Integer deliveryDays,
            @Size(max = 500, message = "{scm.valid.bidDescriptionCannotExceedCharacters}") String remark,
            @NotEmpty(message = "{scm.valid.enterQuotation}") List<@Valid @NotNull BidItem> items) {
    }

    public record BidItem(
            @NotNull Long tenderItemId,
            @NotNull(message = "{scm.valid.enterUnitPrice}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.unitPriceMustGreater}") @Digits(integer = 14, fraction = 4) BigDecimal price,
            @DecimalMin(value = "0", message = "{scm.valid.taxRateCannotNegative}") @DecimalMax(value = "100", message = "{scm.valid.taxRateCannotExceed2}") BigDecimal taxRate,
            @Size(max = 255) String remark) {
    }

    /**
     * 定标
     *
     * @param target      生成采购合同 / 采购订单 / 不生成
     * @param warehouseId 生成采购订单时的收货仓库
     * @param startDate   生成合同的生效日期（默认今天）
     * @param endDate     生成合同的到期日期（默认一年）
     */
    public record Award(
            @NotNull(message = "{scm.valid.selectWinningSupplier}") Long supplierId,
            @Size(max = 255, message = "{scm.valid.awardDescriptionCannotExceedCharacters}") String reason,
            @NotNull(message = "{scm.valid.selectDocumentGenerateAfterAward}") AwardTarget target,
            Long warehouseId,
            LocalDate startDate,
            LocalDate endDate) {
    }

    /** 供应商确认订单：接受时填写承诺交期，拒绝时填写原因 */
    public record OrderConfirm(
            @NotNull(message = "{scm.valid.chooseAcceptReject}") Boolean accept,
            LocalDate promisedDate,
            @Size(max = 255, message = "{scm.valid.descriptionCannotExceedCharacters}") String remark) {
    }

    /** 送货通知 */
    public record Asn(
            @NotNull(message = "{scm.valid.selectPurchaseOrder}") Long orderId,
            @NotNull(message = "{scm.valid.selectShipDate}") LocalDate shipDate,
            LocalDate expectedDate,
            @Size(max = 64) String carrier,
            @Size(max = 64) String trackingNo,
            @Size(max = 255) String remark,
            @NotEmpty(message = "{scm.valid.enterDeliveryLines}") List<@Valid @NotNull AsnItem> items) {
    }

    public record AsnItem(
            @NotNull Long orderItemId,
            @NotNull(message = "{scm.valid.enterDeliveryQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.deliveryQuantityMustGreater}") @Digits(integer = 14, fraction = 4) BigDecimal qty,
            @Size(max = 64) String batchNo,
            @Size(max = 255) String remark) {
    }

    /**
     * 按送货通知收货（生成采购入库单）
     *
     * @param warehouseId 收货仓库，默认订单仓库
     * @param lines       各明细实收数量与库位，不填按送货数量
     */
    public record AsnReceive(
            Long warehouseId,
            @NotNull(message = "{scm.valid.selectReceiptDate}") LocalDate bizDate,
            List<@Valid @NotNull AsnReceiveLine> lines) {
    }

    public record AsnReceiveLine(
            @NotNull Long asnItemId,
            @NotNull @DecimalMin(value = "0", message = "{scm.valid.receivedQuantityCannotLess}") BigDecimal qty,
            Long locationId) {
    }

    /** 开通门户账号 */
    public record PortalAccount(
            @NotBlank(message = "{scm.valid.enterLoginAccount}") @Size(max = 64, message = "{scm.valid.accountCannotExceedCharacters}") String username,
            @NotBlank(message = "{scm.valid.enterContact}") @Size(max = 64) String nickname,
            @Size(max = 32) String phone,
            @NotBlank(message = "{scm.valid.enterInitialPassword}") @Size(max = 64) String password) {
    }

    public record Password(@NotBlank(message = "{scm.valid.enterNewPassword}") @Size(max = 64) String password) {
    }

    public record Status(@NotNull EnableStatus status) {
    }
}
