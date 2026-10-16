package com.lemon.erp.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordRequest(
        String oldPassword,
        @NotBlank(message = "新密码不能为空") String newPassword) {
}
