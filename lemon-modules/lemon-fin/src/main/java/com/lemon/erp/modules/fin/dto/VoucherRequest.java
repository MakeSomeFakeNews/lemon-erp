package com.lemon.erp.modules.fin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改凭证（未审核）：至少两条分录，借贷平衡
 */
public record VoucherRequest(
        @NotNull(message = "请选择凭证日期") LocalDate voucherDate,
        @Min(value = 0, message = "附单据数不能小于0") @Max(value = 999, message = "附单据数不能超过999") Integer attachmentCount,
        @NotNull(message = "请填写分录") @Size(min = 2, max = 200, message = "分录为 2-200 条") List<@Valid @NotNull VoucherEntryRequest> entries) {
}
