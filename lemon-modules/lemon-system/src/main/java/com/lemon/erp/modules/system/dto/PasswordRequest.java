package com.lemon.erp.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordRequest(
        String oldPassword,
        @NotBlank(message = "{system.valid.newPasswordRequired}") String newPassword) {
}
