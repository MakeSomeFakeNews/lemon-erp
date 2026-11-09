package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.OffsetStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 冲账单的请求
 */
public final class OffsetRequests {

    private OffsetRequests() {
    }

    /**
     * 新增 / 修改冲账单（待复核时）
     *
     * @param kind      AR 应收冲账 / AP 应付冲账
     * @param item      冲账项目（业务字典 fin_offset_item）
     * @param amount    冲账金额，正数
     * @param accountId 凭证对方科目，可为空（取凭证科目设置）
     */
    public record Save(
            @NotNull(message = "{fin.valid.selectKind}") BillKind kind,
            @NotNull(message = "{fin.valid.selectBusinessPartner}") Long partnerId,
            @NotNull(message = "{fin.valid.selectDate}") LocalDate offsetDate,
            Long handlerId,
            @NotBlank(message = "{fin.valid.selectOffsetItem}") @Size(max = 32) String item,
            @NotNull(message = "{fin.valid.enterAmount}") @DecimalMin(value = "0.01", message = "{fin.valid.amountMustGreater}")
            @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
            LocalDate beginDate,
            LocalDate endDate,
            @Size(max = 64, message = "{fin.valid.ticketNoTooLong}") String ticketNo,
            Long accountId,
            @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark) {
    }

    /** 列表查询：keyword 匹配单号、票据编号 */
    public record Query(BillKind kind, Long partnerId, OffsetStatus status, String item, String keyword,
                        LocalDate beginDate, LocalDate endDate) {
    }
}
