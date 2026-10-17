package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.modules.wms.model.AssemblyKind;
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
 * 新增 / 修改组装拆卸单
 *
 * @param materialId 成套件
 * @param serialNos  成套件序列号，逗号或换行分隔
 * @param items      子件（库位、批次、序列号同出入库单明细）
 */
public record AssemblyRequest(
        @NotNull(message = "请选择组装或拆卸") AssemblyKind kind,
        @NotNull(message = "请选择仓库") Long warehouseId,
        @NotNull(message = "请选择业务日期") LocalDate bizDate,
        @NotNull(message = "请选择成套件") Long materialId,
        Long locationId,
        @Size(max = 64, message = "批次号不能超过64个字符") String batchNo,
        LocalDate productionDate,
        LocalDate expiryDate,
        String serialNos,
        @NotNull(message = "请填写成套件数量")
        @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
        @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        @NotEmpty(message = "请添加子件") @Size(max = 200, message = "子件不能超过200行") List<@Valid @NotNull StockDocItemRequest> items) {
}
