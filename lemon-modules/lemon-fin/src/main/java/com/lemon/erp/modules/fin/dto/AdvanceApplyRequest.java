package com.lemon.erp.modules.fin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

/**
 * 预收预付冲抵应收应付单
 */
public record AdvanceApplyRequest(LocalDate date,
                                  @NotEmpty(message = "{fin.valid.selectDocumentsOffset}") List<@Valid SettlementItemRequest> items) {
}
