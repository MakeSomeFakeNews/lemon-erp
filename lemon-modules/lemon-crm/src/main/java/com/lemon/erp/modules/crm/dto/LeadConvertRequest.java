package com.lemon.erp.modules.crm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 线索转化：生成客户（含首要联系人），可同时生成商机
 *
 * @param customerCode 新客户编码，留空按编码规则自动生成
 */
public record LeadConvertRequest(
        @Pattern(regexp = "^([A-Za-z0-9_-]{1,32})?$", message = "客户编码只能包含字母、数字、下划线和短横线，最多32个字符") String customerCode,
        Boolean createOpportunity,
        @Size(max = 128, message = "商机名称不能超过128个字符") String opportunityName,
        @DecimalMin(value = "0", message = "金额不能小于0") BigDecimal amount,
        LocalDate expectedDate) {
}
