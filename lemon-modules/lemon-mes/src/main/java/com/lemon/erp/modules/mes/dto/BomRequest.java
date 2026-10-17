package com.lemon.erp.modules.mes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新增 / 修改 BOM：子件与工序整体替换
 *
 * @param isDefault 设为该产品的默认 BOM
 * @param leadDays  生产提前期（天）
 */
public record BomRequest(
        @NotNull(message = "{mes.valid.selectProduct}") Long materialId,
        @NotBlank(message = "{mes.valid.enterVersion}") @Size(max = 16, message = "{mes.valid.versionCannotExceedCharacters}") String version,
        Integer isDefault,
        @Min(value = 0, message = "{mes.valid.leadTimeCannotLess}") @Max(value = 365, message = "{mes.valid.leadTimeCannotExceedDays}") Integer leadDays,
        @Size(max = 255, message = "{mes.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{mes.valid.addComponents}") @Size(max = 500, message = "{mes.valid.componentsCannotExceedLines}") List<@Valid @NotNull BomItemRequest> items,
        @Size(max = 50, message = "{mes.valid.operationsCannotExceed}") List<@Valid @NotNull BomOpRequest> ops) {
}
