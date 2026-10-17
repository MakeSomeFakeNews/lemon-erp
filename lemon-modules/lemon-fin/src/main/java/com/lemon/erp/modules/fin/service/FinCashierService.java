package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.dto.CashierViews;
import com.lemon.erp.modules.fin.model.LedgerRowType;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.mapper.FinCashierMapper;
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
import java.util.stream.Collectors;

/**
 * 出纳管理：现金日记账、银行日记账（按日期逐笔列出现金 / 银行科目的收支、对方科目和余额，每日小计）和资金日报（各账户的期初、收入、支出、余额）。
 * 数据来自凭证，默认包含未审核、未记账的凭证（出纳需要实时余额），也可只看已记账的；另提示还没有生成凭证的收付款
 */
@Service
@RequiredArgsConstructor
public class FinCashierService {

    private static final int MAX_DAYS = 366;

    private final FinCashierMapper cashierMapper;
    private final FinAccountService accountService;

    /**
     * 日记账
     *
     * @param accountId 现金 / 银行科目（可为上级科目，含下级）；为空时为全部现金和银行科目
     * @param posted    只统计已记账凭证
     * @return 行：type 为 OPEN 期初 / ENTRY 分录 / DAY 本日合计 / TOTAL 合计
     */
    public List<CashierViews.JournalRow> journal(Long accountId, LocalDate begin, LocalDate end, boolean posted) {
        checkRange(begin, end);
        List<Long> ids = cashAccountIds(accountId);
        List<CashierViews.JournalRow> rows = new ArrayList<>();
        if (ids.isEmpty()) {
            return rows;
        }
        BigDecimal balance = cashierMapper.sumBefore(ids, begin, posted);
        rows.add(line(LedgerRowType.OPEN, begin, Messages.get("fin.text.openingBalance"), null, null, balance));
        List<CashierViews.JournalEntry> entries = cashierMapper.selectJournal(ids, begin, end, posted);
        Map<Long, String> counters = counterAccounts(entries, ids);
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
            BigDecimal debit = dec(e.getDebit());
            BigDecimal credit = dec(e.getCredit());
            balance = balance.add(debit).subtract(credit);
            dayDebit = dayDebit.add(debit);
            dayCredit = dayCredit.add(credit);
            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
            rows.add(new CashierViews.JournalRow(LedgerRowType.ENTRY, date, e.getSummary(), debit, credit, balance, e.getVoucherId(),
                    e.getVoucherNo(), e.getVoucherStatus(), e.getAccountCode(), e.getAccountName(), counters.get(e.getVoucherId())));
        }
        if (day != null) {
            rows.add(line(LedgerRowType.DAY, day, Messages.get("fin.text.dailyTotal"), dayDebit, dayCredit, balance));
        }
        rows.add(line(LedgerRowType.TOTAL, end, Messages.get("fin.text.total"), totalDebit, totalCredit, balance));
        return rows;
    }

    /**
     * 资金日报：各现金、银行末级科目在日期范围内的期初、收入、支出、余额和笔数，只列有余额或有发生额的账户
     *
     */
    public CashierViews.Daily daily(LocalDate begin, LocalDate end, boolean posted) {
        checkRange(begin, end);
        List<String> cash = accountService.cashCodes();
        List<FinAccount> accounts = accountService.leafOptions().stream().filter(a -> FinAccountService.inScope(a.getCode(), cash)).toList();
        List<CashierViews.DailyRow> rows = new ArrayList<>();
        BigDecimal[] sum = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        long[] counts = {0, 0};
        if (!accounts.isEmpty()) {
            List<Long> ids = accounts.stream().map(FinAccount::getId).toList();
            Map<Long, BigDecimal> open = new HashMap<>();
            cashierMapper.balanceBefore(ids, begin, posted).forEach(r -> open.put(r.getAccountId(), dec(r.getBalance())));
            Map<Long, CashierViews.AccountTotal> sums = cashierMapper.sumByAccount(ids, begin, end, posted).stream()
                    .collect(Collectors.toMap(CashierViews.AccountTotal::getAccountId, r -> r));
            for (FinAccount a : accounts) {
                BigDecimal o = open.getOrDefault(a.getId(), BigDecimal.ZERO);
                CashierViews.AccountTotal s = sums.get(a.getId());
                BigDecimal debit = s == null ? BigDecimal.ZERO : dec(s.getDebit());
                BigDecimal credit = s == null ? BigDecimal.ZERO : dec(s.getCredit());
                if (o.signum() == 0 && s == null) {
                    continue;
                }
                long in = s == null ? 0 : s.getInCount();
                long out = s == null ? 0 : s.getOutCount();
                rows.add(new CashierViews.DailyRow(a.getId(), a.getCode(), a.getName(), o, debit, credit, o.add(debit).subtract(credit), in, out));
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

    /** 还没有生成凭证的收付款（已生效）：{RECEIPT: {count, amount}, PAYMENT: {...}} */
    public Map<SettleKind, CashierViews.CountAmount> unvouchered() {
        Map<SettleKind, CashierViews.CountAmount> result = new LinkedHashMap<>();
        for (CashierViews.KindTotal r : cashierMapper.selectUnvouchered()) {
            result.put(r.getKind(), new CashierViews.CountAmount(r.getCount().intValue(), dec(r.getAmount())));
        }
        return result;
    }

    private List<Long> cashAccountIds(Long accountId) {
        List<String> cash = accountService.cashCodes();
        if (accountId == null) {
            return accountService.listAll().stream().filter(a -> FinAccountService.inScope(a.getCode(), cash)).map(FinAccount::getId).toList();
        }
        FinAccount account = accountService.get(accountId);
        if (!FinAccountService.inScope(account.getCode(), cash)) {
            throw new BizException("fin.error.selectCashBankDepositAccount");
        }
        return accountService.selfAndChildren(account);
    }

    /** 每张凭证的对方科目（现金、银行以外的科目名称，顿号分隔） */
    private Map<Long, String> counterAccounts(List<CashierViews.JournalEntry> entries, List<Long> ids) {
        List<Long> voucherIds = entries.stream().map(CashierViews.JournalEntry::getVoucherId).distinct().toList();
        Map<Long, String> result = new HashMap<>();
        for (int i = 0; i < voucherIds.size(); i += 500) {
            List<Long> chunk = voucherIds.subList(i, Math.min(i + 500, voucherIds.size()));
            cashierMapper.selectCounterAccounts(chunk, ids).forEach(r -> result.merge(r.getVoucherId(),
                    r.getAccountName(), (a, b) -> a.equals(b) ? a : a + "、" + b));
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


    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
