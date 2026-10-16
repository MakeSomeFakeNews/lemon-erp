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
            @Size(max = 255, message = "备注不能超过255个字符") String remark,
            @DecimalMin(value = "0", message = "返利抵扣不能为负数") BigDecimal rebateAmount,
            @NotEmpty(message = "请添加订货物料") @Size(max = 200, message = "明细不能超过200行") List<@Valid @NotNull Line> items) {
    }

    public record Line(
            @NotNull(message = "请选择物料") Long materialId,
            @NotNull(message = "请填写数量") @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
            @Digits(integer = 14, fraction = 4) BigDecimal qty) {
    }

    /** 渠道库存销量上报（同一期间重复上报覆盖） */
    public record Report(
            @Size(max = 255, message = "说明不能超过255个字符") String remark,
            @NotEmpty(message = "请添加上报物料") @Size(max = 500, message = "明细不能超过500行") List<@Valid @NotNull ReportLine> items) {
    }

    public record ReportLine(
            @NotNull(message = "请选择物料") Long materialId,
            @NotNull(message = "请填写销量") @DecimalMin(value = "0", message = "销量不能为负数") @Digits(integer = 14, fraction = 4) BigDecimal sellQty,
            @NotNull(message = "请填写库存") @DecimalMin(value = "0", message = "库存不能为负数") @Digits(integer = 14, fraction = 4) BigDecimal stockQty) {
    }

    /**
     * 外部订单导入的一行（统一格式，同一外部单号的多行为一张订单）
     *
     * @param price 含税单价，为空按价目表
     */
    public record ImportRow(
            @NotBlank(message = "外部单号不能为空") @Size(max = 64, message = "外部单号不能超过64个字符") String externalNo,
            @NotBlank(message = "客户编码不能为空") String customerCode,
            @NotNull(message = "下单日期不能为空") LocalDate orderDate,
            LocalDate deliveryDate,
            @NotBlank(message = "物料编码不能为空") String materialCode,
            @NotNull(message = "数量不能为空") @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
            @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty,
            @DecimalMin(value = "0", message = "单价不能小于0") @Digits(integer = 14, fraction = 4, message = "单价最多4位小数") BigDecimal price,
            @Size(max = 255, message = "备注不能超过255个字符") String remark) {
    }
}
