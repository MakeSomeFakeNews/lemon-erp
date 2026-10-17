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
        @NotNull(message = "{fin.valid.selectVoucherDate}") LocalDate voucherDate,
        @Min(value = 0, message = "{fin.valid.numberAttachmentsCannotLess}") @Max(value = 999, message = "{fin.valid.numberAttachmentsCannotExceed}") Integer attachmentCount,
        @NotNull(message = "{fin.valid.enterVoucherEntries}") @Size(min = 2, max = 200, message = "{fin.valid.voucherMustHaveEntries}") List<@Valid @NotNull VoucherEntryRequest> entries) {
}
