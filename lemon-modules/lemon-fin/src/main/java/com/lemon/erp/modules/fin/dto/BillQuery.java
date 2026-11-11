package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.BillStatus;
import com.lemon.erp.modules.fin.model.BillKind;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * @param keyword 单号或来源单号
 * @param overdue 只看已逾期未核销的
 * @param withAp  挂接应付（仅应收）：同时列出关联供应商（供应商资料中的关联客户）的应付单，金额为负数，往来单位按关联的客户筛选
 */
public record BillQuery(BillKind kind, Long partnerId, BillStatus status, String keyword, Boolean overdue,
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, boolean withAp) {
}
