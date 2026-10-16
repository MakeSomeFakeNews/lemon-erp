package com.lemon.erp.modules.system.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserIdsRequest(
        @NotEmpty(message = "请选择用户") @Size(max = 500, message = "一次最多操作 500 个用户") List<Long> userIds) {
}
