package com.lemon.erp.modules.scm.dto;

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
 * 新增 / 修改请购单（草稿）
 */
public record RequisitionRequest(
        @NotNull(message = "请选择请购日期") LocalDate reqDate,
        @Size(max = 255, message = "说明不能超过255个字符") String remark,
        @NotEmpty(message = "请添加明细") @Size(max = 200, message = "明细不能超过200行") List<@Valid @NotNull Item> items) {

    /**
     * @param supplierId 建议供应商，选填
     */
    public record Item(
            @NotNull(message = "请选择物料") Long materialId,
            @NotNull(message = "请填写数量") @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
            @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty,
            LocalDate requiredDate,
            Long supplierId,
            @Size(max = 255, message = "备注不能超过255个字符") String remark) {
    }
}
