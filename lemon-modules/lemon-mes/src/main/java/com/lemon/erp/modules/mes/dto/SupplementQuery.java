package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.SupplementStatus;

/**
 * @param keyword 申请单号或工单号
 */
public record SupplementQuery(String keyword, Long woId, SupplementStatus status, String reason) {
}
