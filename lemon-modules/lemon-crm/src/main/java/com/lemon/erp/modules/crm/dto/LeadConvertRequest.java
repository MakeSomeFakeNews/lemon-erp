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
        @Pattern(regexp = "^([A-Za-z0-9_-]{1,32})?$", message = "{crm.valid.customerCodeMayContainOnlyLetters}") String customerCode,
        Boolean createOpportunity,
        @Size(max = 128, message = "{crm.valid.opportunityNameCannotExceedCharacters}") String opportunityName,
        @DecimalMin(value = "0", message = "{crm.valid.amountCannotLess}") BigDecimal amount,
        LocalDate expectedDate) {
}
