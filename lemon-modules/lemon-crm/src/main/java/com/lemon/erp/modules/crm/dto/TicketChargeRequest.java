package com.lemon.erp.modules.crm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 服务收费：为工单客户生成应收单（其他业务收入）
 *
 * @param amount  含税金额
 * @param taxRate 税率（%），默认 6
 */
public record TicketChargeRequest(
        @NotNull(message = "请填写收费金额") @DecimalMin(value = "0.01", message = "收费金额必须大于0")
        @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
        @DecimalMin(value = "0", message = "税率不能小于0") @DecimalMax(value = "100", message = "税率不能大于100") BigDecimal taxRate,
        @NotNull(message = "请选择收费日期") LocalDate billDate,
        @Size(max = 255, message = "说明不能超过255个字符") String remark) {
}
