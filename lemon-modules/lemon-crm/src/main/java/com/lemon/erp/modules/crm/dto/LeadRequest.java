package com.lemon.erp.modules.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增 / 修改线索，ownerId 为空时负责人为当前用户
 */
public record LeadRequest(
        @NotBlank(message = "公司名称不能为空") @Size(max = 128, message = "公司名称不能超过128个字符") String company,
        @Size(max = 64, message = "联系人不能超过64个字符") String contactName,
        @Size(max = 64, message = "职务不能超过64个字符") String title,
        @Size(max = 32, message = "电话不能超过32个字符") String phone,
        @Email(message = "邮箱格式不正确") @Size(max = 128, message = "邮箱不能超过128个字符") String email,
        String industry,
        String source,
        Long campaignId,
        @Size(max = 64, message = "地区不能超过64个字符") String region,
        @Size(max = 500, message = "需求描述不能超过500个字符") String requirement,
        Long ownerId) {
}
