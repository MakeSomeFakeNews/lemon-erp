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
        @NotBlank(message = "请填写仓库") String warehouse,
        String location,
        @NotBlank(message = "请填写物料编码") String materialCode,
        @Size(max = 64, message = "批次号不能超过64个字符") String batchNo,
        @NotNull(message = "请填写数量")
        @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
        @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty,
        String serialNos,
        LocalDate productionDate,
        LocalDate expiryDate) {
}
