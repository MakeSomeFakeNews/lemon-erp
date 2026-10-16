package com.lemon.erp.modules.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增 / 修改客户：基本信息保存到客户主数据（与供应链共用），经营信息保存到 CRM。
 * 新增时负责人为当前用户；填写了联系人时同时建立首要联系人
 */
public record CrmCustomerRequest(
        @Pattern(regexp = "^([A-Za-z0-9_-]{1,32})?$", message = "客户编码只能包含字母、数字、下划线和短横线，最多32个字符") String code,
        @NotBlank(message = "客户名称不能为空") @Size(max = 128, message = "客户名称不能超过128个字符") String name,
        String level,
        @Size(max = 64, message = "联系人不能超过64个字符") String contact,
        @Size(max = 32, message = "联系电话不能超过32个字符") String phone,
        @Size(max = 255, message = "地址不能超过255个字符") String address,
        String industry,
        String source,
        String stage,
        @Size(max = 64, message = "地区不能超过64个字符") String region,
        @Size(max = 128, message = "网站不能超过128个字符") String website) {
}
