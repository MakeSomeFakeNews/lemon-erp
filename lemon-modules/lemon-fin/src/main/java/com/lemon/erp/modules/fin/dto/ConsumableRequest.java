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
        @NotNull(message = "{fin.valid.selectIssuingDepartment}") Long deptId,
        Long userId,
        @NotNull(message = "{fin.valid.selectWarehouse}") Long warehouseId,
        @NotNull(message = "{fin.valid.selectIssueDate}") LocalDate useDate,
        @NotNull(message = "{fin.valid.selectExpenseAccount}") Long expenseAccountId,
        @Size(max = 255, message = "{fin.valid.purposeCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{fin.valid.addMaterialsIssue}") @Size(max = 200, message = "{fin.valid.noMoreLines}") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "{fin.valid.selectMaterial}") Long materialId,
            Long locationId,
            @Size(max = 64) String batchNo,
            @NotNull(message = "{fin.valid.enterQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{fin.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4) BigDecimal qty,
            @Size(max = 255) String remark) {
    }
}
