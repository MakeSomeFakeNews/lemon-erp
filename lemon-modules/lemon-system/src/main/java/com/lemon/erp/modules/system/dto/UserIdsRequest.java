package com.lemon.erp.modules.system.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserIdsRequest(
        @NotEmpty(message = "{system.valid.selectUsers}") @Size(max = 500, message = "{system.valid.upUsersProcessedTime}") List<Long> userIds) {
}
