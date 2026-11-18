package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.fin.dto.CashierViews;
import com.lemon.erp.modules.fin.entity.FinCashAccount;
import com.lemon.erp.modules.fin.mapper.FinCashierMapper;
import com.lemon.erp.modules.fin.model.LedgerRowType;
import com.lemon.erp.modules.fin.model.SettleKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 出纳管理：日记账（按日期逐笔列出资金账户的收支、对方科目（按分录配对）和余额，每日小计）、资金日报（各账户的期初、收入、支出、余额）
 * 和核对总账（日记账余额与总账科目余额比对）。
 * 数据来自资金账户入账科目的凭证，默认包含未审核、未记账的凭证（出纳需要实时余额），也可只看已记账的；另提示还没有生成凭证的收付款。
 * 账户有启用日期时，日记账从启用日期起按「日记账期初」计算（之前的凭证不再计入）；没有启用日期时期初取科目期初余额
 */
@Service
@RequiredArgsConstructor
public class FinCashierService {

    private static final int MAX_DAYS = 366;

    private final FinCashierMapper cashierMapper;
    private final FinCashAccountService cashAccountService;

    /**
     * 日记账
     *
     * @param accountId 资金账户的入账科目；为空时为全部资金账户
     * @param posted    只统计已记账凭证
     * @return 行：type 为 OPEN 期初 / ENTRY 分录 / DAY 本日合计 / TOTAL 合计
     */
    public List<CashierViews.JournalRow> journal(Long accountId, LocalDate begin, LocalDate end, boolean posted) {
        checkRange(begin, end);
        List<FinCashAccount> accounts = accountId == null ? cashAccountService.list() : List.of(cashAccountService.byAccountId(accountId));
        List<CashierViews.JournalRow> rows = new ArrayList<>();
        if (accounts.isEmpty()) {
            return rows;
        }
        Map<Long, FinCashAccount> byAccount = accounts.stream().collect(Collectors.toMap(FinCashAccount::getAccountId, Function.identity()));
        BigDecimal balance = journalOpening(accounts, begin, posted).values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        rows.add(line(LedgerRowType.OPEN, begin, Messages.get("fin.text.openingBalance"), null, null, balance));
        List<CashierViews.JournalEntry> entries = cashierMapper.selectJournal(scopes(accounts), begin, end, posted);
        Map<Long, String> counters = counterAccounts(entries);
        BigDecimal dayDebit = BigDecimal.ZERO;
        BigDecimal dayCredit = BigDecimal.ZERO;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        LocalDate day = null;
        for (CashierViews.JournalEntry e : entries) {
            LocalDate date = e.getEntryDate();
            if (day != null && !day.equals(date)) {
                rows.add(line(LedgerRowType.DAY, day, Messages.get("fin.text.dailyTotal"), dayDebit, dayCredit, balance));
                dayDebit = BigDecimal.ZERO;
                dayCredit = BigDecimal.ZERO;
            }
            day = date;
            BigDecimal debit = Decimals.nz(e.getDebit());
            BigDecimal credit = Decimals.nz(e.getCredit());
            balance = balance.add(debit).subtract(credit);
            dayDebit = dayDebit.add(debit);
            dayCredit = dayCredit.add(credit);
            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
            FinCashAccount account = byAccount.get(e.getAccountId());
            rows.add(new CashierViews.JournalRow(LedgerRowType.ENTRY, date, e.getSummary(), debit, credit, balance, e.getVoucherId(),
                    e.getVoucherNo(), e.getVoucherStatus(), e.getAccountCode(), account == null ? e.getAccountName() : account.getName(),
                    counters.get(e.getEntryId()), e.getSourceType(), e.getSourceNo()));
        }
        if (day != null) {
            rows.add(line(LedgerRowType.DAY, day, Messages.get("fin.text.dailyTotal"), dayDebit, dayCredit, balance));
        }
        rows.add(line(LedgerRowType.TOTAL, end, Messages.get("fin.text.total"), totalDebit, totalCredit, balance));
        return rows;
    }

    /** 资金日报：各资金账户在日期范围内的期初、收入、支出、余额和笔数，只列有余额或有发生额的账户 */
    public CashierViews.Daily daily(LocalDate begin, LocalDate end, boolean posted) {
        checkRange(begin, end);
        List<FinCashAccount> accounts = cashAccountService.list();
        List<CashierViews.DailyRow> rows = new ArrayList<>();
        BigDecimal[] sum = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        long[] counts = {0, 0};
        if (!accounts.isEmpty()) {
            Map<Long, BigDecimal> open = journalOpening(accounts, begin, posted);
            Map<Long, CashierViews.AccountTotal> sums = sums(accounts, begin, end, posted);
            for (FinCashAccount a : accounts) {
                BigDecimal o = open.getOrDefault(a.getAccountId(), BigDecimal.ZERO);
                CashierViews.AccountTotal s = sums.get(a.getAccountId());
                BigDecimal debit = s == null ? BigDecimal.ZERO : Decimals.nz(s.getDebit());
                BigDecimal credit = s == null ? BigDecimal.ZERO : Decimals.nz(s.getCredit());
                if (o.signum() == 0 && s == null) {
                    continue;
                }
                long in = s == null ? 0 : s.getInCount();
                long out = s == null ? 0 : s.getOutCount();
                rows.add(new CashierViews.DailyRow(a.getAccountId(), a.getAccountCode(), a.getName(), o, debit, credit,
                        o.add(debit).subtract(credit), in, out, a.getType()));
                sum[0] = sum[0].add(o);
                sum[1] = sum[1].add(debit);
                sum[2] = sum[2].add(credit);
                sum[3] = sum[3].add(o.add(debit).subtract(credit));
                counts[0] += in;
                counts[1] += out;
            }
        }
        return new CashierViews.Daily(rows, new CashierViews.DailyTotal(sum[0], sum[1], sum[2], sum[3], counts[0], counts[1]));
    }

    /**
     * 核对总账：各资金账户的出纳日记账（启用日期起，日记账期初 + 全部凭证，含未审核、未记账）与总账（入账科目的科目期初 + 已记账凭证）
     * 在日期范围内的期初、借贷发生额和期末比对。差额来自日记账期初与总账不一致、未记账凭证；还没有生成凭证的收付款两边都不包含，单独列出
     */
    public List<CashierViews.CheckRow> check(LocalDate begin, LocalDate end) {
        checkRange(begin, end);
        List<FinCashAccount> accounts = cashAccountService.list();
        List<CashierViews.CheckRow> rows = new ArrayList<>();
        if (accounts.isEmpty()) {
            return rows;
        }
        LocalDate after = end.plusDays(1);
        Map<Long, BigDecimal> journalOpen = journalOpening(accounts, begin, false);
        Map<Long, BigDecimal> journalClose = journalOpening(accounts, after, false);
        Map<Long, BigDecimal> journalPostedClose = journalOpening(accounts, after, true);
        Map<Long, CashierViews.AccountTotal> journalSums = sums(accounts, begin, end, false);
        // 总账：入账科目的全部凭证（不受启用日期限制），含科目期初，只统计已记账的
        List<CashierViews.Scope> ledgerScopes = accounts.stream().map(a -> new CashierViews.Scope(a.getAccountId(), null)).toList();
        List<Long> ids = accounts.stream().map(FinCashAccount::getAccountId).toList();
        Map<Long, BigDecimal> ledgerOpen = toMap(cashierMapper.balanceBefore(ledgerScopes, ids, begin, true));
        Map<Long, BigDecimal> ledgerClose = toMap(cashierMapper.balanceBefore(ledgerScopes, ids, after, true));
        Map<Long, CashierViews.AccountTotal> ledgerSums = cashierMapper.sumByAccount(ledgerScopes, begin, end, true).stream()
                .collect(Collectors.toMap(CashierViews.AccountTotal::getAccountId, r -> r));
        Map<Long, CashierViews.AccountTotal> unvouchered = cashierMapper.sumUnvoucheredByAccount(end).stream()
                .collect(Collectors.toMap(CashierViews.AccountTotal::getAccountId, r -> r));
        for (FinCashAccount a : accounts) {
            Long id = a.getAccountId();
            BigDecimal jo = journalOpen.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal jc = journalClose.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal lo = ledgerOpen.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal lc = ledgerClose.getOrDefault(id, BigDecimal.ZERO);
            CashierViews.AccountTotal js = journalSums.get(id);
            CashierViews.AccountTotal ls = ledgerSums.get(id);
            BigDecimal openingDiff = a.getEnableDate() == null ? BigDecimal.ZERO
                    : Decimals.nz(a.getJournalOpening()).subtract(posted(a.getAccountId(), a.getEnableDate()));
            CashierViews.AccountTotal u = unvouchered.get(id);
            BigDecimal diff = jc.subtract(lc);
            rows.add(new CashierViews.CheckRow(a.getId(), a.getName(), a.getType(), id, a.getAccountCode(), a.getAccountName(), a.getEnableDate(),
                    jo, debit(js), credit(js), jc, lo, debit(ls), credit(ls), lc, diff,
                    jc.subtract(journalPostedClose.getOrDefault(id, BigDecimal.ZERO)), openingDiff,
                    u == null ? 0 : u.getInCount(), u == null ? BigDecimal.ZERO : Decimals.nz(u.getBalance()), diff.signum() == 0));
        }
        return rows;
    }

    /** 还没有生成凭证的收付款（已生效）：{RECEIPT: {count, amount}, PAYMENT: {...}} */
    public Map<SettleKind, CashierViews.CountAmount> unvouchered() {
        Map<SettleKind, CashierViews.CountAmount> result = new LinkedHashMap<>();
        for (CashierViews.KindTotal r : cashierMapper.selectUnvouchered()) {
            result.put(r.getKind(), new CashierViews.CountAmount(r.getCount().intValue(), Decimals.nz(r.getAmount())));
        }
        return result;
    }

    /** 各账户在日期之前的日记账余额：有启用日期的为日记账期初 + 启用日期起的凭证，没有的为科目期初 + 凭证；入账科目 ID → 余额 */
    private Map<Long, BigDecimal> journalOpening(List<FinCashAccount> accounts, LocalDate date, boolean posted) {
        List<Long> openingIds = accounts.stream().filter(a -> a.getEnableDate() == null).map(FinCashAccount::getAccountId).toList();
        Map<Long, BigDecimal> result = new HashMap<>(toMap(cashierMapper.balanceBefore(scopes(accounts), openingIds, date, posted)));
        for (FinCashAccount a : accounts) {
            if (a.getEnableDate() != null) {
                result.merge(a.getAccountId(), Decimals.nz(a.getJournalOpening()), BigDecimal::add);
            }
        }
        return result;
    }

    private Map<Long, CashierViews.AccountTotal> sums(List<FinCashAccount> accounts, LocalDate begin, LocalDate end, boolean posted) {
        return cashierMapper.sumByAccount(scopes(accounts), begin, end, posted).stream()
                .collect(Collectors.toMap(CashierViews.AccountTotal::getAccountId, r -> r));
    }

    /** 科目在日期之前的总账余额（科目期初 + 已记账凭证） */
    private BigDecimal posted(Long accountId, LocalDate date) {
        return cashierMapper.sumBefore(List.of(new CashierViews.Scope(accountId, null)), List.of(accountId), date, true);
    }

    private static List<CashierViews.Scope> scopes(List<FinCashAccount> accounts) {
        return accounts.stream().map(a -> new CashierViews.Scope(a.getAccountId(), a.getEnableDate())).toList();
    }

    private static Map<Long, BigDecimal> toMap(List<CashierViews.AccountTotal> rows) {
        return rows.stream().filter(r -> Objects.nonNull(r.getAccountId()))
                .collect(Collectors.toMap(CashierViews.AccountTotal::getAccountId, r -> Decimals.nz(r.getBalance())));
    }

    private static BigDecimal debit(CashierViews.AccountTotal t) {
        return t == null ? BigDecimal.ZERO : Decimals.nz(t.getDebit());
    }

    private static BigDecimal credit(CashierViews.AccountTotal t) {
        return t == null ? BigDecimal.ZERO : Decimals.nz(t.getCredit());
    }

    /** 每笔分录的对方科目：按分录配对（规则见 {@link CounterAccounts}），分录 ID → 科目名称（顿号分隔） */
    private Map<Long, String> counterAccounts(List<CashierViews.JournalEntry> entries) {
        List<Long> voucherIds = entries.stream().map(CashierViews.JournalEntry::getVoucherId).distinct().toList();
        Map<Long, String> result = new HashMap<>();
        for (int i = 0; i < voucherIds.size(); i += 500) {
            List<Long> chunk = voucherIds.subList(i, Math.min(i + 500, voucherIds.size()));
            result.putAll(CounterAccounts.of(cashierMapper.selectVoucherLines(chunk)));
        }
        return result;
    }

    private static CashierViews.JournalRow line(LedgerRowType type, LocalDate date, String summary, BigDecimal debit, BigDecimal credit, BigDecimal balance) {
        return CashierViews.JournalRow.total(type, date, summary, debit, credit, balance);
    }

    private static void checkRange(LocalDate begin, LocalDate end) {
        if (begin == null || end == null) {
            throw new BizException("fin.error.selectDateRange");
        }
        if (end.isBefore(begin)) {
            throw new BizException("fin.error.endDateCannotEarlierStartDate");
        }
        if (ChronoUnit.DAYS.between(begin, end) >= MAX_DAYS) {
            throw new BizException("fin.error.dateRangeCannotExceedOneYear");
        }
    }
}
