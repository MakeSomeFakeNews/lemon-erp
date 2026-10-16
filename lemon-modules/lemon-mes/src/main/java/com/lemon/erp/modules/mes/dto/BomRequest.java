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
        @NotNull(message = "请选择产品") Long materialId,
        @NotBlank(message = "请填写版本号") @Size(max = 16, message = "版本号不能超过16个字符") String version,
        Integer isDefault,
        @Min(value = 0, message = "提前期不能小于0") @Max(value = 365, message = "提前期不能超过365天") Integer leadDays,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        @NotEmpty(message = "请添加子件") @Size(max = 500, message = "子件不能超过500行") List<@Valid @NotNull BomItemRequest> items,
        @Size(max = 50, message = "工序不能超过50道") List<@Valid @NotNull BomOpRequest> ops) {
}
