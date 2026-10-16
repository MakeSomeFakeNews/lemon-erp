package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.FundDirection;
import com.lemon.erp.modules.fin.model.FundSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资金计划的一笔预计收支
 *
 * @param refNo 来源单号
 */
public record FundPlanEntry(FundDirection direction, FundSource source, Long refId, String refNo, String partnerName,
                            LocalDate dueDate, BigDecimal amount) {
}
