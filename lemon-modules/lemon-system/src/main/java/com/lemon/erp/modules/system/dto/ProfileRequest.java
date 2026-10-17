package com.lemon.erp.modules.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 个人资料修改，只允许修改这三项
 */
public record ProfileRequest(
        @Size(max = 64, message = "{system.valid.nicknameCannotExceedCharacters}") String nickname,
        @Email(message = "{system.valid.invalidEmailFormat}") String email,
        @Size(max = 32, message = "{system.valid.mobileNumberCannotExceedCharacters}") String phone) {
}
