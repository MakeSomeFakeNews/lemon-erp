package com.lemon.erp.modules.fin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
        @NotBlank(message = "请选择种类") String kind,
        @NotNull(message = "请选择往来单位") Long partnerId,
        @NotNull(message = "请选择日期") LocalDate settleDate,
        @NotNull(message = "请选择结算账户") Long accountId,
        @Size(max = 16) String method,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        @NotEmpty(message = "请选择要核销的单据") @Size(max = 500) List<@Valid @NotNull SettlementItemRequest> items,
        @jakarta.validation.constraints.DecimalMin(value = "0", inclusive = false, message = "汇率必须大于0") java.math.BigDecimal exchangeRate) {

    public SettlementRequest(String kind, Long partnerId, LocalDate settleDate, Long accountId, String method, String remark,
                             List<SettlementItemRequest> items) {
        this(kind, partnerId, settleDate, accountId, method, remark, items, null);
    }
}
