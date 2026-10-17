package com.lemon.erp.modules.fin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.modules.fin.model.LedgerRowType;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.modules.fin.model.VoucherStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 出纳管理（日记账、资金日报、未生成凭证的收付款）的返回结构。数据库查询结果用带 setter 的类（MyBatis 按列名映射）
 */
public final class CashierViews {

    private CashierViews() {
    }

    /**
     * 日记账的一行：type 为 OPEN 期初 / ENTRY 分录 / DAY 本日合计 / TOTAL 合计；凭证、科目、对方科目只有分录行有
     */
    public record JournalRow(LedgerRowType type, LocalDate entryDate, String summary, BigDecimal debit, BigDecimal credit,
                             BigDecimal balance,
                             @JsonInclude(JsonInclude.Include.NON_NULL) Long voucherId,
                             @JsonInclude(JsonInclude.Include.NON_NULL) String voucherNo,
                             @JsonInclude(JsonInclude.Include.NON_NULL) VoucherStatus voucherStatus,
                             @JsonInclude(JsonInclude.Include.NON_NULL) String accountCode,
                             @JsonInclude(JsonInclude.Include.NON_NULL) String accountName,
                             @JsonInclude(JsonInclude.Include.NON_NULL) String counterAccounts) {

        /** 期初、合计行 */
        public static JournalRow total(LedgerRowType type, LocalDate date, String summary, BigDecimal debit, BigDecimal credit,
                                       BigDecimal balance) {
            return new JournalRow(type, date, summary, debit, credit, balance, null, null, null, null, null, null);
        }
    }

    /** 现金、银行科目的凭证分录 */
    @Data
    public static class JournalEntry {
        private LocalDate entryDate;
        private Long voucherId;
        private String voucherNo;
        private VoucherStatus voucherStatus;
        private String summary;
        private Long accountId;
        private String accountCode;
        private String accountName;
        private BigDecimal debit;
        private BigDecimal credit;
    }

    /** 凭证的对方科目 */
    @Data
    public static class CounterAccount {
        private Long voucherId;
        private String accountName;
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

    /** 资金日报的一行（现金、银行末级科目） */
    public record DailyRow(Long accountId, String code, String name, BigDecimal open, BigDecimal debit, BigDecimal credit,
                           BigDecimal close, long inCount, long outCount) {
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

    /** 笔数和金额 */
    public record CountAmount(int count, BigDecimal amount) {
    }
}
