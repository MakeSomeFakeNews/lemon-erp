package com.lemon.erp.modules.wms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 期初库存导入的一行
 *
 * @param warehouse    仓库编码或名称
 * @param location     库位编码，不填为未指定库位
 * @param materialCode 物料编码
 * @param serialNos    序列号，逗号或换行分隔
 */
public record OpeningStockRow(
        @NotBlank(message = "{wms.valid.enterWarehouse}") String warehouse,
        String location,
        @NotBlank(message = "{wms.valid.enterMaterialCode}") String materialCode,
        @Size(max = 64, message = "{wms.valid.batchNoCannotExceedCharacters}") String batchNo,
        @NotNull(message = "{wms.valid.enterQuantity}")
        @DecimalMin(value = "0", inclusive = false, message = "{wms.valid.quantityMustGreater}")
        @Digits(integer = 14, fraction = 4, message = "{wms.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
        String serialNos,
        LocalDate productionDate,
        LocalDate expiryDate) {
}
