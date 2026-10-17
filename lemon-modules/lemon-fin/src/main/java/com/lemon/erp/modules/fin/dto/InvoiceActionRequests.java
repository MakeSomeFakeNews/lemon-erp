package com.lemon.erp.modules.fin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 发票勾稽、进项认证的请求
 */
public final class InvoiceActionRequests {

    private InvoiceActionRequests() {
    }

    /** 勾稽：发票对应的应收 / 应付单及金额 */
    public record Match(@NotEmpty(message = "{fin.valid.selectDocumentsMatch}") List<InvoiceRequest.@Valid @NotNull Item> items) {
    }

    /** 进项发票认证：period 为认证期间（yyyyMM） */
    public record Certify(@NotEmpty List<Long> ids, Integer period) {
    }
}
