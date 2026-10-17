package com.lemon.erp.modules.wms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 盘点明细
 *
 * @param actualQty 实盘数量，草稿中可以为空，确认时必填；序列号物料以序列号个数为准
 * @param serialNos 实盘的全部序列号（序列号物料），逗号或换行分隔
 */
public record CheckItemRequest(
        @NotNull(message = "{wms.valid.selectMaterial}") Long materialId,
        Long locationId,
        @Size(max = 64, message = "{wms.valid.batchNoCannotExceedCharacters}") String batchNo,
        @DecimalMin(value = "0", message = "{wms.valid.countedQuantityCannotLess}")
        @Digits(integer = 14, fraction = 4, message = "{wms.valid.quantityAllowsMostDecimalPlaces}") BigDecimal actualQty,
        String serialNos,
        @Size(max = 255, message = "{wms.valid.remarkCannotExceedCharacters}") String remark) {
}
