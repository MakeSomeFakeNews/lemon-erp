package com.lemon.erp.modules.fin.dto;

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
 * 低值易耗品领用
 *
 * @param deptId           领用部门（费用归属）
 * @param expenseAccountId 费用科目：管理费用、制造费用、销售费用等
 */
public record ConsumableRequest(
        @NotNull(message = "请选择领用部门") Long deptId,
        Long userId,
        @NotNull(message = "请选择仓库") Long warehouseId,
        @NotNull(message = "请选择领用日期") LocalDate useDate,
        @NotNull(message = "请选择费用科目") Long expenseAccountId,
        @Size(max = 255, message = "用途不能超过255个字符") String remark,
        @NotEmpty(message = "请添加领用物料") @Size(max = 200, message = "明细不能超过200行") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "请选择物料") Long materialId,
            Long locationId,
            @Size(max = 64) String batchNo,
            @NotNull(message = "请填写数量") @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
            @Digits(integer = 14, fraction = 4) BigDecimal qty,
            @Size(max = 255) String remark) {
    }
}
