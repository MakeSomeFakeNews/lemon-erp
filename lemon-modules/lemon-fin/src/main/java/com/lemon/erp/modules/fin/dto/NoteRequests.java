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
            @NotNull(message = "请选择票据类型") NoteType noteType,
            @NotBlank(message = "请填写票据号码") @Size(max = 64, message = "票据号码不能超过64个字符") String noteNo,
            Boolean electronic,
            @NotNull(message = "请选择往来单位") Long partnerId,
            @NotNull(message = "请填写票面金额") @DecimalMin(value = "0", inclusive = false, message = "票面金额必须大于 0")
            @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
            @NotNull(message = "请选择出票日期") LocalDate issueDate,
            @NotNull(message = "请选择到期日期") LocalDate dueDate,
            @Size(max = 128, message = "出票人不能超过128个字符") String drawer,
            @Size(max = 128, message = "承兑人不能超过128个字符") String acceptor,
            @NotNull(message = "请选择日期") LocalDate actionDate,
            @NotEmpty(message = "请选择要核销的单据") @Size(max = 500) List<@Valid @NotNull SettlementItemRequest> items,
            @Size(max = 255, message = "备注不能超过255个字符") String remark) {
    }

    /** 背书转让：把持有的应收票据转给供应商支付货款，核销应付单，核销合计须等于票面金额 */
    public record Endorse(
            @NotNull(message = "请选择供应商") Long supplierId,
            @NotNull(message = "请选择背书日期") LocalDate actionDate,
            @NotEmpty(message = "请选择要核销的应付单") @Size(max = 500) List<@Valid @NotNull SettlementItemRequest> items,
            @Size(max = 255, message = "备注不能超过255个字符") String remark) {
    }

    /**
     * 贴现、托收、兑付、拒付
     *
     * @param accountId 银行科目（贴现、托收入账，兑付付款）；拒付不需要
     * @param interest  贴现息（只有贴现）
     */
    public record Settle(
            @NotNull(message = "请选择日期") LocalDate actionDate,
            Long accountId,
            @DecimalMin(value = "0", message = "贴现息不能小于 0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数")
            BigDecimal interest,
            @Size(max = 255, message = "备注不能超过255个字符") String remark) {
    }
}
