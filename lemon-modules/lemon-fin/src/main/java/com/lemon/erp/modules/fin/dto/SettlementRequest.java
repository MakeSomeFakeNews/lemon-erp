package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.SettleKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 新增收款 / 付款单：金额为各核销金额合计
 *
 * @param kind      RECEIPT 收款 / PAYMENT 付款
 * @param accountId 结算科目（库存现金、银行存款的末级科目）
 */
public record SettlementRequest(
        @NotNull(message = "{fin.valid.selectKind}") SettleKind kind,
        @NotNull(message = "{fin.valid.selectBusinessPartner}") Long partnerId,
        @NotNull(message = "{fin.valid.selectDate}") LocalDate settleDate,
        @NotNull(message = "{fin.valid.selectSettlementAccount}") Long accountId,
        @Size(max = 16) String method,
        @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{fin.valid.selectDocumentsWriteOff}") @Size(max = 500) List<@Valid @NotNull SettlementItemRequest> items,
        @jakarta.validation.constraints.DecimalMin(value = "0", inclusive = false, message = "{fin.valid.exchangeRateMustGreater}") java.math.BigDecimal exchangeRate) {

    public SettlementRequest(SettleKind kind, Long partnerId, LocalDate settleDate, Long accountId, String method, String remark,
                             List<SettlementItemRequest> items) {
        this(kind, partnerId, settleDate, accountId, method, remark, items, null);
    }
}
