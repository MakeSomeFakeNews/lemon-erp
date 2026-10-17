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
        @NotNull(message = "{wms.valid.selectAssembleDisassemble}") AssemblyKind kind,
        @NotNull(message = "{wms.valid.selectWarehouse}") Long warehouseId,
        @NotNull(message = "{wms.valid.selectBusinessDate}") LocalDate bizDate,
        @NotNull(message = "{wms.valid.selectKit}") Long materialId,
        Long locationId,
        @Size(max = 64, message = "{wms.valid.batchNoCannotExceedCharacters}") String batchNo,
        LocalDate productionDate,
        LocalDate expiryDate,
        String serialNos,
        @NotNull(message = "{wms.valid.enterKitQuantity}")
        @DecimalMin(value = "0", inclusive = false, message = "{wms.valid.quantityMustGreater}")
        @Digits(integer = 14, fraction = 4, message = "{wms.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
        @Size(max = 255, message = "{wms.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{wms.valid.addComponents}") @Size(max = 200, message = "{wms.valid.componentsCannotExceedLines}") List<@Valid @NotNull StockDocItemRequest> items) {
}
