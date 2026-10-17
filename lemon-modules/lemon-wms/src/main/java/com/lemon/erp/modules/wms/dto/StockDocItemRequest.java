package com.lemon.erp.modules.wms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 出入库单明细
 *
 * @param locationId     库位；调拨为调出库位，不填为未指定库位
 * @param toLocationId   调入库位，仅调拨
 * @param batchNo        批次号，启用批次管理的物料必填
 * @param productionDate 生产日期，仅入库
 * @param expiryDate     有效期至，仅入库
 * @param serialNos      序列号，逗号或换行分隔；启用序列号管理的物料，个数须与数量一致
 */
public record StockDocItemRequest(
        @NotNull(message = "{wms.valid.selectMaterial}") Long materialId,
        Long locationId,
        Long toLocationId,
        @Size(max = 64, message = "{wms.valid.batchNoCannotExceedCharacters}") String batchNo,
        LocalDate productionDate,
        LocalDate expiryDate,
        @NotNull(message = "{wms.valid.enterQuantity}")
        @DecimalMin(value = "0", inclusive = false, message = "{wms.valid.quantityMustGreater}")
        @Digits(integer = 14, fraction = 4, message = "{wms.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
        String serialNos,
        @Size(max = 255, message = "{wms.valid.remarkCannotExceedCharacters}") String remark) {
}
