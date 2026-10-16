package com.lemon.erp.modules.scm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 入库 / 出库 / 退货明细：对应一行订单明细，同一订单明细可以拆成多行（不同库位、批次）
 *
 * @param productionDate 生产日期，仅采购入库
 * @param expiryDate     有效期至，仅采购入库
 * @param serialNos      序列号，逗号或换行分隔
 */
public record TradeDocItemRequest(
        @NotNull(message = "请选择订单明细") Long orderItemId,
        Long locationId,
        @Size(max = 64, message = "批次号不能超过64个字符") String batchNo,
        LocalDate productionDate,
        LocalDate expiryDate,
        @NotNull(message = "请填写数量")
        @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
        @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty,
        String serialNos,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
