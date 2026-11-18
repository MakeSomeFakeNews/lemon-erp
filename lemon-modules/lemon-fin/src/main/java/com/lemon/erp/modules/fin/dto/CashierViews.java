package com.lemon.erp.modules.fin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.modules.fin.model.CashAccountType;
import com.lemon.erp.modules.fin.model.LedgerRowType;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.fin.model.VoucherStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 出纳管理（日记账、资金日报、核对总账、未生成凭证的收付款）的返回结构。数据库查询结果用带 setter 的类（MyBatis 按列名映射）
 */
public final class CashierViews {

    private CashierViews() {
    }

    /**
     * 日记账的一行：type 为 OPEN 期初 / ENTRY 分录 / DAY 本日合计 / TOTAL 合计；凭证、科目、对方科目、来源单据只有分录行有
     */
    public record JournalRow(LedgerRowType type, LocalDate entryDate, String summary, BigDecimal debit, BigDecimal credit,
                             BigDecimal balance,
                             @JsonInclude(JsonInclude.Include.NON_NULL) Long voucherId,
                             @JsonInclude(JsonInclude.Include.NON_NULL) String voucherNo,
                             @JsonInclude(JsonInclude.Include.NON_NULL) VoucherStatus voucherStatus,
                             @JsonInclude(JsonInclude.Include.NON_NULL) String accountCode,
                             @JsonInclude(JsonInclude.Include.NON_NULL) String accountName,
                             @JsonInclude(JsonInclude.Include.NON_NULL) String counterAccounts,
                             @JsonInclude(JsonInclude.Include.NON_NULL) VoucherSource sourceType,
                             @JsonInclude(JsonInclude.Include.NON_NULL) String sourceNo) {

        /** 期初、合计行 */
        public static JournalRow total(LedgerRowType type, LocalDate date, String summary, BigDecimal debit, BigDecimal credit,
                                       BigDecimal balance) {
            return new JournalRow(type, date, summary, debit, credit, balance, null, null, null, null, null, null, null, null);
        }
    }

    /** 现金、银行科目的凭证分录 */
    @Data
    public static class JournalEntry {
        private Long entryId;
        private LocalDate entryDate;
        private Long voucherId;
        private String voucherNo;
        private VoucherStatus voucherStatus;
        private VoucherSource sourceType;
        private String sourceNo;
        private String summary;
        private Long accountId;
        private String accountCode;
        private String accountName;
        private BigDecimal debit;
        private BigDecimal credit;
    }

    /** 凭证分录（确定对方科目用） */
    @Data
    public static class VoucherLine {
        private Long voucherId;
        private Long entryId;
        private String accountName;
        private BigDecimal debit;
        private BigDecimal credit;
    }

    /** 按科目汇总：借贷发生额与笔数，或期初余额 */
    @Data
    public static class AccountTotal {
        private Long accountId;
        private BigDecimal debit;
        private BigDecimal credit;
        private Long inCount;
        private Long outCount;
        private BigDecimal balance;
    }

    /**
     * 资金日报的一行（资金账户）
     *
     * @param accountId 入账科目 ID
     * @param code      入账科目编码
     * @param name      资金账户名称
     */
    public record DailyRow(Long accountId, String code, String name, BigDecimal open, BigDecimal debit, BigDecimal credit,
                           BigDecimal close, long inCount, long outCount, CashAccountType type) {
    }

    public record DailyTotal(BigDecimal open, BigDecimal debit, BigDecimal credit, BigDecimal close, long inCount, long outCount) {
    }

    /** 资金日报 */
    public record Daily(List<DailyRow> rows, DailyTotal total) {
    }

    /** 未生成凭证的已生效收付款按种类汇总（数据库查询结果） */
    @Data
    public static class KindTotal {
        private SettleKind kind;
        private Long count;
        private BigDecimal amount;
    }

    /**
     * 查询范围：资金账户的入账科目及其启用日期（为空表示从建账开始）。只统计启用日期当天及以后的凭证，
     * 启用日期之前的收支已包含在日记账期初中（MyBatis 参数）
     */
    @Data
    @AllArgsConstructor
    public static class Scope {
        private Long accountId;
        private LocalDate from;
    }

    /**
     * 核对总账的一行：出纳日记账（启用日期起，日记账期初 + 全部凭证，含未审核、未记账）与总账（科目期初 + 已记账凭证）比对
     *
     * @param cashAccountId     资金账户
     * @param accountId         入账科目
     * @param journalOpen       日记账期初（开始日期之前）
     * @param ledgerOpen        总账期初
     * @param difference        期末差额 = 日记账期末 − 总账期末
     * @param unposted          日记账中未记账凭证的净额（截至结束日期），总账不包含
     * @param openingDifference 日记账期初与总账在启用日期之前余额的差额，没有启用日期时为 0
     * @param unvoucheredCount  截至结束日期还没有生成凭证的收付款笔数（日记账、总账都不包含）
     * @param unvoucheredAmount 其净额（收款为正、付款为负）
     */
    public record CheckRow(Long cashAccountId, String name, CashAccountType type, Long accountId, String accountCode, String accountName,
                           LocalDate enableDate, BigDecimal journalOpen, BigDecimal journalDebit, BigDecimal journalCredit, BigDecimal journalClose,
                           BigDecimal ledgerOpen, BigDecimal ledgerDebit, BigDecimal ledgerCredit, BigDecimal ledgerClose,
                           BigDecimal difference, BigDecimal unposted, BigDecimal openingDifference,
                           long unvoucheredCount, BigDecimal unvoucheredAmount, boolean matched) {
    }

    /** 笔数和金额 */
    public record CountAmount(int count, BigDecimal amount) {
    }
}
