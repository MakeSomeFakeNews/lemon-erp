package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.fin.dto.AccountSum;
import com.lemon.erp.modules.fin.dto.AuxBalanceRow;
import com.lemon.erp.modules.fin.dto.BalanceRow;
import com.lemon.erp.modules.fin.dto.LedgerRow;
import com.lemon.erp.modules.fin.dto.TrialBalance;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.mapper.FinVoucherEntryMapper;
import com.lemon.erp.modules.fin.model.AuxItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.fin.model.FinConstants.previous;
import static com.lemon.erp.modules.fin.model.FinConstants.toYearMonth;

/**
 * 账簿：只统计已记账凭证（财务报表见 FinStatementService）。
 * <ul>
 *   <li>科目余额表：期初、本期借贷发生额、期末，上级科目汇总下级</li>
 *   <li>明细账：科目（含下级）的逐笔分录与滚动余额</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FinReportService {

    private final FinVoucherEntryMapper entryMapper;
    private final FinAccountService accountService;

    // ---------------------------------------------------------------- 科目余额表

    public TrialBalance trialBalance(int begin, int end, boolean showZero) {
        checkRange(begin, end);
        List<FinAccount> accounts = accountService.listAll();
        Map<Long, AccountSum> open = toMap(entryMapper.sumByAccount(null, previous(begin), false));
        Map<Long, AccountSum> current = toMap(entryMapper.sumByAccount(begin, end, false));
        List<BalanceRow> rows = new ArrayList<>();
        BigDecimal[] totals = zeros(6);
        for (FinAccount a : accounts) {
            List<FinAccount> leaves = leavesOf(a, accounts);
            BigDecimal od = sum(leaves, open, true), oc = sum(leaves, open, false);
            BigDecimal d = sum(leaves, current, true), c = sum(leaves, current, false);
            BalanceRow row = new BalanceRow();
            row.setAccountId(a.getId());
            row.setCode(a.getCode());
            row.setName(a.getName());
            row.setCategory(a.getCategory());
            row.setDirection(a.getDirection());
            row.setLevel((a.getCode().length() - 4) / 2 + 1);
            row.setLeaf(a.getLeaf());
            boolean debitSide = "DEBIT".equals(a.getDirection());
            row.setOpenBalance(debitSide ? od.subtract(oc) : oc.subtract(od));
            row.setDebit(d);
            row.setCredit(c);
            row.setCloseBalance(debitSide ? od.add(d).subtract(oc).subtract(c) : oc.add(c).subtract(od).subtract(d));
            boolean zero = row.getOpenBalance().signum() == 0 && d.signum() == 0 && c.signum() == 0 && row.getCloseBalance().signum() == 0;
            if (showZero || !zero) {
                rows.add(row);
            }
            if (a.getParentId() == 0) {
                // 一级科目合计：期初、期末余额按实际借贷方向分列
                addBySide(totals, 0, od.subtract(oc));
                totals[2] = totals[2].add(d);
                totals[3] = totals[3].add(c);
                addBySide(totals, 4, od.add(d).subtract(oc).subtract(c));
            }
        }
        return TrialBalance.of(rows, totals);
    }

    // ---------------------------------------------------------------- 明细账

    /** 辅助核算余额：按辅助核算项目 + 科目统计本期发生额和期末余额 */
    public List<AuxBalanceRow> auxBalance(AuxItem dimension, int begin, int end, Long accountId) {
        if (dimension == null) {
            throw new BizException("fin.error.selectAuxiliaryAccountingType");
        }
        return entryMapper.selectAuxBalance(dimension, begin, end, accountId);
    }

    public List<LedgerRow> ledger(Long accountId, int begin, int end) {
        checkRange(begin, end);
        FinAccount account = accountService.get(accountId);
        List<Long> ids = accountService.selfAndChildren(account);
        boolean debitSide = "DEBIT".equals(account.getDirection());
        Map<Long, AccountSum> open = toMap(entryMapper.sumByAccount(null, previous(begin), false));
        BigDecimal od = BigDecimal.ZERO, oc = BigDecimal.ZERO;
        for (Long id : ids) {
            AccountSum s = open.get(id);
            if (s != null) {
                od = od.add(s.getDebit());
                oc = oc.add(s.getCredit());
            }
        }
        BigDecimal balance = debitSide ? od.subtract(oc) : oc.subtract(od);
        List<LedgerRow> rows = new ArrayList<>();
        LedgerRow first = new LedgerRow();
        first.setType("OPEN");
        first.setEntryDate(toYearMonth(begin).atDay(1));
        first.setSummary(Messages.get("fin.text.openingBalance"));
        first.setBalance(balance);
        first.setSide(side(balance, debitSide));
        rows.add(first);
        BigDecimal totalDebit = BigDecimal.ZERO, totalCredit = BigDecimal.ZERO;
        for (LedgerRow r : entryMapper.selectLedger(ids, begin, end)) {
            r.setType("ENTRY");
            balance = balance.add(debitSide ? r.getDebit().subtract(r.getCredit()) : r.getCredit().subtract(r.getDebit()));
            r.setBalance(balance);
            r.setSide(side(balance, debitSide));
            totalDebit = totalDebit.add(r.getDebit());
            totalCredit = totalCredit.add(r.getCredit());
            rows.add(r);
        }
        LedgerRow total = new LedgerRow();
        total.setType("TOTAL");
        total.setEntryDate(toYearMonth(end).atEndOfMonth());
        total.setSummary(Messages.get("fin.text.periodTotal"));
        total.setDebit(totalDebit);
        total.setCredit(totalCredit);
        total.setBalance(balance);
        total.setSide(side(balance, debitSide));
        rows.add(total);
        return rows;
    }

    /** 借方科目 = 借 - 贷，贷方科目 = 贷 - 借 */
    private static BigDecimal net(AccountSum s, boolean debitSide) {
        if (s == null) {
            return BigDecimal.ZERO;
        }
        return debitSide ? s.getDebit().subtract(s.getCredit()) : s.getCredit().subtract(s.getDebit());
    }

    private static List<FinAccount> leavesOf(FinAccount account, List<FinAccount> all) {
        if (account.getLeaf()) {
            return List.of(account);
        }
        return all.stream().filter(a -> a.getLeaf() && a.getCode().startsWith(account.getCode())).toList();
    }

    private static BigDecimal sum(List<FinAccount> accounts, Map<Long, AccountSum> sums, boolean debit) {
        BigDecimal total = BigDecimal.ZERO;
        for (FinAccount a : accounts) {
            AccountSum s = sums.get(a.getId());
            if (s != null) {
                total = total.add(debit ? s.getDebit() : s.getCredit());
            }
        }
        return total;
    }

    /** 余额按实际方向计入借方合计（正）或贷方合计（负） */
    private static void addBySide(BigDecimal[] totals, int offset, BigDecimal debitMinusCredit) {
        if (debitMinusCredit.signum() >= 0) {
            totals[offset] = totals[offset].add(debitMinusCredit);
        } else {
            totals[offset + 1] = totals[offset + 1].add(debitMinusCredit.negate());
        }
    }

    private static String side(BigDecimal balance, boolean debitSide) {
        if (balance.signum() == 0) {
            return Messages.get("fin.text.bal");
        }
        return Messages.get((balance.signum() > 0) == debitSide ? "fin.label.dr" : "fin.label.cr");
    }

    private static Map<Long, AccountSum> toMap(List<AccountSum> list) {
        return list.stream().collect(Collectors.toMap(AccountSum::getAccountId, s -> s, (a, b) -> a, HashMap::new));
    }

    private static BigDecimal[] zeros(int n) {
        BigDecimal[] a = new BigDecimal[n];
        java.util.Arrays.fill(a, BigDecimal.ZERO);
        return a;
    }

    private static void checkRange(int begin, int end) {
        toYearMonth(begin);
        toYearMonth(end);
        if (begin > end) {
            throw new BizException("fin.error.startPeriodCannotLaterEndPeriod");
        }
    }
}
