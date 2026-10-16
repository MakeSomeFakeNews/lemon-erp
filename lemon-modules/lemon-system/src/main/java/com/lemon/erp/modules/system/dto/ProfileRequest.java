package com.lemon.erp.modules.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 个人资料修改，只允许修改这三项
 */
public record ProfileRequest(
        @Size(max = 64, message = "昵称不能超过64个字符") String nickname,
        @Email(message = "邮箱格式不正确") String email,
        @Size(max = 32, message = "手机号不能超过32个字符") String phone) {
}
