package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.framework.code.DocNoCatalog.DateFormat;
import com.lemon.erp.framework.code.DocNoCatalog.ResetCycle;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CodeRuleRequest(
        @NotNull(message = "{wms.valid.enterPrefix}")
        @Pattern(regexp = "^[A-Z0-9-]{1,8}$", message = "{wms.valid.prefixMayOnlyContainUppercaseLetters}") String prefix,
        @NotNull(message = "{wms.valid.selectDateFormat}") DateFormat dateFormat,
        @NotNull(message = "{wms.valid.enterSequenceLength}") @Min(value = 3, message = "{wms.valid.sequenceLengthMust}") @Max(value = 8, message = "{wms.valid.sequenceLengthMust}") Integer seqLength,
        @NotNull(message = "{wms.valid.selectSequenceResetCycle}") ResetCycle resetCycle) {
}
