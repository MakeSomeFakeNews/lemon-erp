package com.lemon.erp.modules.crm.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 分配 / 转移客户：ownerId 为空表示放回公海
 */
public record AssignRequest(@NotEmpty(message = "请选择客户") List<Long> customerIds, Long ownerId) {
}
