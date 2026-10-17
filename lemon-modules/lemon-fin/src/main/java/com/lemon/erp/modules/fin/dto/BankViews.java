package com.lemon.erp.modules.fin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.modules.fin.entity.FinBankStatement;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 银行对账的返回结构
 */
public final class BankViews {

    private BankViews() {
    }

    /** 银行科目的凭证分录：金额借正贷负，matched 为 1 表示已与银行流水勾对（数据库查询结果，为空的列不输出） */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BankEntry {
        private Long entryId;
        private Long voucherId;
        private String voucherNo;
        private LocalDate voucherDate;
        private String summary;
        private BigDecimal amount;
        private Integer matched;
    }

    /**
     * 银行存款余额调节表
     *
     * @param bankIn   银行已收、企业未收
     * @param bankOut  银行已付、企业未付
     * @param bookIn   企业已收、银行未收
     * @param bookOut  企业已付、银行未付
     * @param bankOnly 未勾对的银行流水
     * @param bookOnly 未勾对的凭证分录
     */
    public record Reconciliation(LocalDate asOf, BigDecimal bookBalance, BigDecimal bankBalance, BigDecimal bankIn, BigDecimal bankOut,
                                 BigDecimal bookIn, BigDecimal bookOut, BigDecimal adjustedBook, BigDecimal adjustedBank,
                                 boolean balanced, List<FinBankStatement> bankOnly, List<BankEntry> bookOnly) {
    }
}
