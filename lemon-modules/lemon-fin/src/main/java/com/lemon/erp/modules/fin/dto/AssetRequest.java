package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.AssetStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增 / 修改固定资产卡片：已计提折旧的资产不能修改原值、残值率、年限、开始使用日期
 *
 * @param usefulMonths       使用年限（月）
 * @param expenseAccountId   折旧费用科目（末级）
 * @param openingMonths      期初已计提月数（系统上线前已使用的资产），仅新增时有效
 * @param openingAccumulated 期初累计折旧，仅新增时有效
 */
public record AssetRequest(
        @NotBlank(message = "请填写资产编码") @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "资产编码只能包含字母、数字、下划线和短横线") String code,
        @NotBlank(message = "请填写资产名称") @Size(max = 64) String name,
        @NotBlank(message = "请选择类别") String category,
        @Size(max = 64) String model,
        Long deptId,
        Long custodianId,
        @Size(max = 128, message = "存放地点不能超过128个字符") String location,
        @NotNull(message = "请选择开始使用日期") LocalDate useDate,
        @NotNull(message = "请填写原值") @DecimalMin(value = "0", inclusive = false, message = "原值必须大于0")
        @Digits(integer = 16, fraction = 2) BigDecimal originalValue,
        @DecimalMin(value = "0", message = "残值率不能小于0") @DecimalMax(value = "100", message = "残值率不能大于100") BigDecimal residualRate,
        @NotNull(message = "请填写使用年限") @Min(value = 1, message = "使用年限至少1个月") @Max(value = 1200) Integer usefulMonths,
        @NotNull(message = "请选择折旧费用科目") Long expenseAccountId,
        AssetStatus status,
        @Size(max = 255) String remark,
        @Min(value = 0, message = "期初已计提月数不能小于0") Integer openingMonths,
        @DecimalMin(value = "0", message = "期初累计折旧不能小于0") @Digits(integer = 16, fraction = 2) BigDecimal openingAccumulated,
        Long supplierId,
        @DecimalMin(value = "0", message = "进项税额不能小于0") @Digits(integer = 16, fraction = 2) BigDecimal taxAmount) {
}
