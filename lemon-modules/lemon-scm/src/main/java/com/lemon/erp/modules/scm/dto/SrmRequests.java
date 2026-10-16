package com.lemon.erp.modules.scm.dto;

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
            @NotBlank(message = "请填写合同名称") @Size(max = 128, message = "合同名称不能超过128个字符") String name,
            @NotNull(message = "请选择供应商") Long supplierId,
            @NotNull(message = "请选择合同类型") ContractType contractType,
            @NotNull(message = "请选择签订日期") LocalDate signDate,
            @NotNull(message = "请选择生效日期") LocalDate startDate,
            @NotNull(message = "请选择到期日期") LocalDate endDate,
            @DecimalMin(value = "0", inclusive = false, message = "金额上限必须大于0") @Digits(integer = 16, fraction = 2) BigDecimal amountLimit,
            @Min(value = 0, message = "付款天数不能小于0") @Max(value = 3650, message = "付款天数不能超过3650") Integer paymentDays,
            @Size(max = 255, message = "付款条款不能超过255个字符") String paymentTerms,
            @Size(max = 500, message = "备注不能超过500个字符") String remark,
            @NotEmpty(message = "请添加合同物料") @Size(max = 500, message = "明细不能超过500行") List<@Valid @NotNull ContractItem> items) {
    }

    public record ContractItem(
            @NotNull(message = "请选择物料") Long materialId,
            @NotNull(message = "请填写合同单价") @DecimalMin(value = "0", message = "单价不能小于0") @Digits(integer = 14, fraction = 4) BigDecimal price,
            @DecimalMin(value = "0", message = "税率不能小于0") @DecimalMax(value = "100", message = "税率不能超过100") BigDecimal taxRate,
            @DecimalMin(value = "0", inclusive = false, message = "数量上限必须大于0") @Digits(integer = 14, fraction = 4) BigDecimal qtyLimit,
            @Size(max = 255) String remark) {
    }

    public record Close(@NotBlank(message = "请填写关闭原因") @Size(max = 255, message = "关闭原因不能超过255个字符") String reason) {
    }

    /** 招标 */
    public record Tender(
            @NotBlank(message = "请填写招标标题") @Size(max = 128, message = "标题不能超过128个字符") String title,
            @NotNull(message = "请选择投标截止时间") LocalDateTime deadline,
            @Size(max = 500, message = "招标说明不能超过500个字符") String remark,
            @NotEmpty(message = "请添加招标物料") @Size(max = 200, message = "明细不能超过200行") List<@Valid @NotNull TenderItem> items,
            @NotEmpty(message = "请选择邀请的供应商") @Size(max = 50, message = "邀请的供应商不能超过50家") List<@NotNull Long> supplierIds) {
    }

    public record TenderItem(
            @NotNull(message = "请选择物料") Long materialId,
            @NotNull(message = "请填写数量") @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0") @Digits(integer = 14, fraction = 4) BigDecimal qty,
            LocalDate requiredDate,
            @Size(max = 255) String remark) {
    }

    /**
     * 投标：门户提交时忽略 supplierId（取当前账号的供应商），采购代录时必填
     */
    public record Bid(
            Long supplierId,
            @Min(value = 0, message = "交货期不能小于0") @Max(value = 3650, message = "交货期不能超过3650天") Integer deliveryDays,
            @Size(max = 500, message = "投标说明不能超过500个字符") String remark,
            @NotEmpty(message = "请填写报价") List<@Valid @NotNull BidItem> items) {
    }

    public record BidItem(
            @NotNull Long tenderItemId,
            @NotNull(message = "请填写单价") @DecimalMin(value = "0", inclusive = false, message = "单价必须大于0") @Digits(integer = 14, fraction = 4) BigDecimal price,
            @DecimalMin(value = "0", message = "税率不能小于0") @DecimalMax(value = "100", message = "税率不能超过100") BigDecimal taxRate,
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
            @NotNull(message = "请选择中标供应商") Long supplierId,
            @Size(max = 255, message = "定标说明不能超过255个字符") String reason,
            @NotNull(message = "请选择定标后生成的单据") AwardTarget target,
            Long warehouseId,
            LocalDate startDate,
            LocalDate endDate) {
    }

    /** 供应商确认订单：接受时填写承诺交期，拒绝时填写原因 */
    public record OrderConfirm(
            @NotNull(message = "请选择接受或拒绝") Boolean accept,
            LocalDate promisedDate,
            @Size(max = 255, message = "说明不能超过255个字符") String remark) {
    }

    /** 送货通知 */
    public record Asn(
            @NotNull(message = "请选择采购订单") Long orderId,
            @NotNull(message = "请选择发货日期") LocalDate shipDate,
            LocalDate expectedDate,
            @Size(max = 64) String carrier,
            @Size(max = 64) String trackingNo,
            @Size(max = 255) String remark,
            @NotEmpty(message = "请填写送货明细") List<@Valid @NotNull AsnItem> items) {
    }

    public record AsnItem(
            @NotNull Long orderItemId,
            @NotNull(message = "请填写送货数量") @DecimalMin(value = "0", inclusive = false, message = "送货数量必须大于0") @Digits(integer = 14, fraction = 4) BigDecimal qty,
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
            @NotNull(message = "请选择收货日期") LocalDate bizDate,
            List<@Valid @NotNull AsnReceiveLine> lines) {
    }

    public record AsnReceiveLine(
            @NotNull Long asnItemId,
            @NotNull @DecimalMin(value = "0", message = "实收数量不能小于0") BigDecimal qty,
            Long locationId) {
    }

    /** 开通门户账号 */
    public record PortalAccount(
            @NotBlank(message = "请填写登录账号") @Size(max = 64, message = "账号不能超过64个字符") String username,
            @NotBlank(message = "请填写联系人") @Size(max = 64) String nickname,
            @Size(max = 32) String phone,
            @NotBlank(message = "请填写初始密码") @Size(max = 64) String password) {
    }

    public record Password(@NotBlank(message = "请填写新密码") @Size(max = 64) String password) {
    }

    public record Status(@NotNull @Min(0) @Max(1) Integer status) {
    }
}
