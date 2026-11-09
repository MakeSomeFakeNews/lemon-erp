package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.SettleKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增收款 / 付款单：金额为各核销金额合计
 *
 * @param kind        RECEIPT 收款 / PAYMENT 付款
 * @param accountId   结算科目（库存现金、银行存款的末级科目）
 * @param totalAmount 本次收款 / 付款总额，可为空（= 核销合计）；不能小于核销合计，多出的部分须选择转预收 / 预付
 * @param toAdvance   多收 / 多付的部分转为预收 / 预付款（同一事务生成预收预付单）
 */
public record SettlementRequest(
        @NotNull(message = "{fin.valid.selectKind}") SettleKind kind,
        @NotNull(message = "{fin.valid.selectBusinessPartner}") Long partnerId,
        @NotNull(message = "{fin.valid.selectDate}") LocalDate settleDate,
        @NotNull(message = "{fin.valid.selectSettlementAccount}") Long accountId,
        @Size(max = 16) String method,
        @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{fin.valid.selectDocumentsWriteOff}") @Size(max = 500) List<@Valid @NotNull SettlementItemRequest> items,
        @DecimalMin(value = "0", inclusive = false, message = "{fin.valid.exchangeRateMustGreater}") BigDecimal exchangeRate,
        @DecimalMin(value = "0", message = "{fin.valid.amountMustGreater}")
        @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal totalAmount,
        Boolean toAdvance) {

    public SettlementRequest(SettleKind kind, Long partnerId, LocalDate settleDate, Long accountId, String method, String remark,
                             List<SettlementItemRequest> items) {
        this(kind, partnerId, settleDate, accountId, method, remark, items, null, null, null);
    }
}
