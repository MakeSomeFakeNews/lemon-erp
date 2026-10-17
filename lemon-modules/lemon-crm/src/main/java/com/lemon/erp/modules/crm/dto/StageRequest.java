package com.lemon.erp.modules.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 推进销售阶段；输单需要填写原因
 */
public record StageRequest(@NotBlank(message = "{crm.valid.selectSalesStage}") String stage,
                           @Size(max = 255, message = "{crm.valid.lostReasonCannotExceedCharacters}") String lostReason) {
}
