package com.lemon.erp.modules.crm.dto;

import com.lemon.erp.modules.crm.model.OpportunityStage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 推进销售阶段；输单需要填写原因
 */
public record StageRequest(@NotNull(message = "{crm.valid.selectSalesStage}") OpportunityStage stage,
                           @Size(max = 255, message = "{crm.valid.lostReasonCannotExceedCharacters}") String lostReason) {
}
