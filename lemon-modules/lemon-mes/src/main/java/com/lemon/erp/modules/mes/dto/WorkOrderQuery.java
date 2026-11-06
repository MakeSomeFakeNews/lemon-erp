package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * @param woNo      工单号，模糊匹配
 * @param keyword   产品编码或名称
 * @param beginDate 计划开工日期范围
 * @param overdueAt 只看逾期的工单：已下达、生产中且计划完工日期早于该日期（一般为今天）
 */
public record WorkOrderQuery(String woNo, String keyword, WorkOrderStatus status,
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate overdueAt) {
}
