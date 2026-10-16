package com.lemon.erp.modules.fin.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * @param keyword 单号或来源单号
 * @param overdue 只看已逾期未核销的
 */
public record BillQuery(String kind, Long partnerId, String status, String keyword, Boolean overdue,
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
}
