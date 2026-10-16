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
        @NotNull(message = "请选择物料") Long materialId,
        Long locationId,
        @Size(max = 64, message = "批次号不能超过64个字符") String batchNo,
        @DecimalMin(value = "0", message = "实盘数量不能小于0")
        @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal actualQty,
        String serialNos,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
