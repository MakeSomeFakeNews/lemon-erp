package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 期初往来导入的一行（应收 / 应付）
 *
 * @param partner 客户 / 供应商的编码或名称
 * @param dueDate 到期日，不填按往来单位账期计算
 * @param amount  未收 / 未付金额，可为负数（如期初预收冲减）
 */
public record OpeningBillRow(
        @NotBlank(message = "请填写往来单位") String partner,
        @NotNull(message = "请填写单据日期") LocalDate billDate,
        LocalDate dueDate,
        @NotNull(message = "请填写金额") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
