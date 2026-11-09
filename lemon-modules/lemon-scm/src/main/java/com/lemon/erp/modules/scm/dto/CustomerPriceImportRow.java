package com.lemon.erp.modules.scm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 客户执行价导入的一行（与导出的列一致，导出的文件修改后可直接导入）
 *
 * @param customerCode 客户编码
 * @param materialCode 物料编码
 * @param price        执行价（含税、基本单位、本位币）
 * @param taxRate      税率（%），为空表示开单时不改税率
 */
public record CustomerPriceImportRow(
        @NotBlank(message = "{scm.valid.customerPriceCustomerCode}") String customerCode,
        @NotBlank(message = "{scm.valid.customerPriceMaterialCode}") String materialCode,
        @NotNull(message = "{scm.valid.customerPricePrice}")
        @DecimalMin(value = "0", message = "{scm.valid.customerPricePrice}")
        @Digits(integer = 14, fraction = 4, message = "{scm.valid.customerPricePriceDigits}") BigDecimal price,
        @DecimalMin(value = "0", message = "{scm.valid.customerPriceTaxRate}")
        @DecimalMax(value = "100", message = "{scm.valid.customerPriceTaxRate}") BigDecimal taxRate,
        @Size(max = 255, message = "{scm.valid.customerPriceRemark}") String remark) {
}
