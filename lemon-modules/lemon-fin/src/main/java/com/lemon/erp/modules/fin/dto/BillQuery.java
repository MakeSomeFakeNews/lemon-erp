package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.BillStatus;
import com.lemon.erp.modules.fin.model.BillKind;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * @param keyword 单号或来源单号
 * @param overdue 只看已逾期未核销的
 */
public record BillQuery(BillKind kind, Long partnerId, BillStatus status, String keyword, Boolean overdue,
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
}
