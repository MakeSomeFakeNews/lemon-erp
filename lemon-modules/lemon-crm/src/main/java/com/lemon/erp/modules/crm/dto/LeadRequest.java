package com.lemon.erp.modules.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增 / 修改线索，ownerId 为空时负责人为当前用户
 */
public record LeadRequest(
        @NotBlank(message = "{crm.valid.companyNameRequired}") @Size(max = 128, message = "{crm.valid.companyNameCannotExceedCharacters}") String company,
        @Size(max = 64, message = "{crm.valid.contactCannotExceedCharacters}") String contactName,
        @Size(max = 64, message = "{crm.valid.titleCannotExceedCharacters}") String title,
        @Size(max = 32, message = "{crm.valid.phoneCannotExceedCharacters}") String phone,
        @Email(message = "{crm.valid.invalidEmailFormat}") @Size(max = 128, message = "{crm.valid.emailCannotExceedCharacters}") String email,
        String industry,
        String source,
        Long campaignId,
        @Size(max = 64, message = "{crm.valid.regionCannotExceedCharacters}") String region,
        @Size(max = 500, message = "{crm.valid.requirementCannotExceedCharacters}") String requirement,
        Long ownerId) {
}
