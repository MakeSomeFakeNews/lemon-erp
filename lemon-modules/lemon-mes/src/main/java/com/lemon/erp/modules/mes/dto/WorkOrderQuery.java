package com.lemon.erp.modules.mes.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * @param woNo      工单号，模糊匹配
 * @param keyword   产品编码或名称
 * @param beginDate 计划开工日期范围
 */
public record WorkOrderQuery(String woNo, String keyword, String status,
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
}
