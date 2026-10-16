package com.lemon.erp.modules.mes.dto;

/**
 * @param keyword 申请单号或工单号
 */
public record SupplementQuery(String keyword, Long woId, String status, String reason) {
}
