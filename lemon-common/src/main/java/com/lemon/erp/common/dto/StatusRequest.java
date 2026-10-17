package com.lemon.erp.common.dto;

import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.constraints.NotNull;

/** 启用 / 停用请求：{"status": 1 | 0}，其他取值反序列化时提示字段取值不正确 */
public record StatusRequest(@NotNull(message = "{common.valid.statusRequired}") EnableStatus status) {
}
