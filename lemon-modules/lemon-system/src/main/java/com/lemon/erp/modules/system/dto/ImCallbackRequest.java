package com.lemon.erp.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 授权回调：平台跳回系统访问地址时带的授权码和 state */
public record ImCallbackRequest(@NotBlank(message = "{system.valid.missingAuthorizationCode}") @Size(max = 512) String code,
                                @NotBlank(message = "{system.valid.missingState}") @Size(max = 64) String state) {
}
