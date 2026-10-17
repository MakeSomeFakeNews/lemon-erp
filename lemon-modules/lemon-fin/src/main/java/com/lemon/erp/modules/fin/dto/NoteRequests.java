package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.NoteType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 票据操作的请求
 */
public final class NoteRequests {

    private NoteRequests() {
    }

    /**
     * 收票（客户以承兑汇票付款）/ 开票（以应付票据支付供应商）：登记票据并核销往来单据，核销合计须等于票面金额
     *
     * @param partnerId  收票的客户 / 开票的供应商
     * @param actionDate 收票 / 开票日期
     * @param items      核销的应收单 / 应付单
     */
    public record Register(
            @NotNull(message = "{fin.valid.selectNoteType}") NoteType noteType,
            @NotBlank(message = "{fin.valid.enterNoteNumber}") @Size(max = 64, message = "{fin.valid.noteNumberCannotExceedCharacters}") String noteNo,
            Boolean electronic,
            @NotNull(message = "{fin.valid.selectBusinessPartner}") Long partnerId,
            @NotNull(message = "{fin.valid.enterFaceAmount}") @DecimalMin(value = "0", inclusive = false, message = "{fin.valid.faceAmountMustGreater}")
            @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
            @NotNull(message = "{fin.valid.selectIssueDate2}") LocalDate issueDate,
            @NotNull(message = "{fin.valid.selectExpiryDate}") LocalDate dueDate,
            @Size(max = 128, message = "{fin.valid.drawerCannotExceedCharacters}") String drawer,
            @Size(max = 128, message = "{fin.valid.acceptorCannotExceedCharacters}") String acceptor,
            @NotNull(message = "{fin.valid.selectDate}") LocalDate actionDate,
            @NotEmpty(message = "{fin.valid.selectDocumentsWriteOff}") @Size(max = 500) List<@Valid @NotNull SettlementItemRequest> items,
            @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark) {
    }

    /** 背书转让：把持有的应收票据转给供应商支付货款，核销应付单，核销合计须等于票面金额 */
    public record Endorse(
            @NotNull(message = "{fin.valid.selectSupplier}") Long supplierId,
            @NotNull(message = "{fin.valid.selectEndorsementDate}") LocalDate actionDate,
            @NotEmpty(message = "{fin.valid.selectPayablesWriteOff}") @Size(max = 500) List<@Valid @NotNull SettlementItemRequest> items,
            @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark) {
    }

    /**
     * 贴现、托收、兑付、拒付
     *
     * @param accountId 银行科目（贴现、托收入账，兑付付款）；拒付不需要
     * @param interest  贴现息（只有贴现）
     */
    public record Settle(
            @NotNull(message = "{fin.valid.selectDate}") LocalDate actionDate,
            Long accountId,
            @DecimalMin(value = "0", message = "{fin.valid.discountInterestCannotLess}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}")
            BigDecimal interest,
            @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark) {
    }
}
