package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.framework.code.DocNoCatalog.DateFormat;
import com.lemon.erp.framework.code.DocNoCatalog.ResetCycle;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CodeRuleRequest(
        @NotNull(message = "请填写前缀")
        @Pattern(regexp = "^[A-Z0-9-]{1,8}$", message = "前缀只能包含大写字母、数字和短横线，最多8个字符") String prefix,
        @NotNull(message = "请选择日期格式") DateFormat dateFormat,
        @NotNull(message = "请填写流水位数") @Min(value = 3, message = "流水位数为 3 ~ 8") @Max(value = 8, message = "流水位数为 3 ~ 8") Integer seqLength,
        @NotNull(message = "请选择流水重置周期") ResetCycle resetCycle) {
}
