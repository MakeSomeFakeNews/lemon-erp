package com.lemon.erp.modules.crm.dto;

import com.lemon.erp.modules.crm.model.CustomerStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增 / 修改客户：基本信息保存到客户主数据（与供应链共用），经营信息保存到 CRM。
 * 新增时负责人为当前用户；填写了联系人时同时建立首要联系人
 */
public record CrmCustomerRequest(
        @Pattern(regexp = "^([A-Za-z0-9_-]{1,32})?$", message = "{crm.valid.customerCodeMayContainOnlyLetters}") String code,
        @NotBlank(message = "{crm.valid.customerNameRequired}") @Size(max = 128, message = "{crm.valid.customerNameCannotExceedCharacters}") String name,
        String level,
        @Size(max = 64, message = "{crm.valid.contactCannotExceedCharacters}") String contact,
        @Size(max = 32, message = "{crm.valid.contactPhoneCannotExceedCharacters}") String phone,
        @Size(max = 255, message = "{crm.valid.addressCannotExceedCharacters}") String address,
        String industry,
        String source,
        CustomerStage stage,
        @Size(max = 64, message = "{crm.valid.regionCannotExceedCharacters}") String region,
        @Size(max = 128, message = "{crm.valid.websiteCannotExceedCharacters}") String website) {
}
