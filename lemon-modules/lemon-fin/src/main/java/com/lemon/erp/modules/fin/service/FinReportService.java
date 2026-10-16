package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.fin.dto.AccountSum;
import com.lemon.erp.modules.fin.dto.AuxBalanceRow;
import com.lemon.erp.modules.fin.dto.BalanceRow;
import com.lemon.erp.modules.fin.dto.LedgerRow;
import com.lemon.erp.modules.fin.dto.ReportLine;
import com.lemon.erp.modules.fin.dto.TrialBalance;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.mapper.FinVoucherEntryMapper;
import com.lemon.erp.modules.fin.model.AccountCategory;
import com.lemon.erp.modules.fin.model.AuxItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.fin.model.FinConstants.previous;
import static com.lemon.erp.modules.fin.model.FinConstants.toYearMonth;

/**
 * 账簿与报表：只统计已记账凭证。
 * <ul>
 *   <li>科目余额表：期初、本期借贷发生额、期末，上级科目汇总下级</li>
 *   <li>明细账：科目（含下级）的逐笔分录与滚动余额</li>
 *   <li>资产负债表：期末、年初余额；未分配利润含尚未结转的损益</li>
 *   <li>利润表：本期金额、本年累计，不含结转损益凭证</li>
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
            throw new BizException("请选择辅助核算类型");
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
        first.setSummary("期初余额");
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
        total.setSummary("本期合计");
        total.setDebit(totalDebit);
        total.setCredit(totalCredit);
        total.setBalance(balance);
        total.setSide(side(balance, debitSide));
        rows.add(total);
        return rows;
    }

    // ---------------------------------------------------------------- 资产负债表

    /** 资产负债表：amount 期末余额，amount2 年初余额 */
    public List<ReportLine> balanceSheet(int period) {
        toYearMonth(period);
        List<FinAccount> leaves = accountService.listAll().stream().filter(FinAccount::getLeaf).toList();
        Map<Long, AccountSum> close = toMap(entryMapper.sumByAccount(null, period, false));
        Map<Long, AccountSum> yearOpen = toMap(entryMapper.sumByAccount(null, (period / 100 - 1) * 100 + 12, false));
        Sheet sheet = new Sheet(leaves, close, yearOpen);
        List<ReportLine> lines = new ArrayList<>();
        lines.add(sheet.title("资产"));
        BigDecimal[] assets = sheet.section(lines, AccountCategory.ASSET, true, new String[][]{
                {"货币资金", "1001", "1002"}, {"应收账款", "1122"}, {"预付账款", "1123"}, {"其他应收款", "1221"},
                {"存货", "1403", "1405", "5"}, {"固定资产（净值）", "1601", "1602"}, {"固定资产清理", "1606"}}, "其他资产");
        lines.add(new ReportLine("资产总计", 0, assets[0], assets[1], true));
        lines.add(sheet.title("负债"));
        BigDecimal[] liabilities = sheet.section(lines, AccountCategory.LIABILITY, false, new String[][]{
                {"短期借款", "2001"}, {"应付账款", "2202"}, {"预收账款", "2203"}, {"应付职工薪酬", "2211"}, {"应交税费", "2221"},
                {"其他应付款", "2241"}}, "其他负债");
        lines.add(new ReportLine("负债合计", 0, liabilities[0], liabilities[1], true));
        lines.add(sheet.title("所有者权益"));
        BigDecimal[] equity = sheet.section(lines, AccountCategory.EQUITY, false, new String[][]{
                {"实收资本", "4001"}, {"未分配利润", "4103", "4104", "6"}}, "其他权益");
        lines.add(new ReportLine("所有者权益合计", 0, equity[0], equity[1], true));
        lines.add(new ReportLine("负债和所有者权益总计", 0, liabilities[0].add(equity[0]), liabilities[1].add(equity[1]), true));
        return lines;
    }

    /** 按行定义（编码前缀）归集末级科目余额，未匹配的同类科目计入「其他」行 */
    private static final class Sheet {

        private final List<FinAccount> leaves;
        private final Map<Long, AccountSum> close;
        private final Map<Long, AccountSum> yearOpen;
        private final java.util.Set<Long> used = new java.util.HashSet<>();

        Sheet(List<FinAccount> leaves, Map<Long, AccountSum> close, Map<Long, AccountSum> yearOpen) {
            this.leaves = leaves;
            this.close = close;
            this.yearOpen = yearOpen;
        }

        ReportLine title(String name) {
            return new ReportLine(name, 0, null, null, true);
        }

        /** 成本类（5 开头）计入存货，损益类（6 开头）计入未分配利润，所以按编码前缀而不是类别匹配 */
        BigDecimal[] section(List<ReportLine> lines, AccountCategory category, boolean debitSide, String[][] defs, String otherName) {
            BigDecimal total = BigDecimal.ZERO, total2 = BigDecimal.ZERO;
            for (String[] def : defs) {
                List<String> prefixes = List.of(def).subList(1, def.length);
                BigDecimal[] v = amount(a -> prefixes.stream().anyMatch(p -> a.getCode().startsWith(p)), debitSide);
                lines.add(new ReportLine(def[0], 1, v[0], v[1], false));
                total = total.add(v[0]);
                total2 = total2.add(v[1]);
            }
            BigDecimal[] other = amount(a -> category.name().equals(a.getCategory()), debitSide);
            if (other[0].signum() != 0 || other[1].signum() != 0) {
                lines.add(new ReportLine(otherName, 1, other[0], other[1], false));
                total = total.add(other[0]);
                total2 = total2.add(other[1]);
            }
            return new BigDecimal[]{total, total2};
        }

        private BigDecimal[] amount(Predicate<FinAccount> match, boolean debitSide) {
            BigDecimal v = BigDecimal.ZERO, v2 = BigDecimal.ZERO;
            for (FinAccount a : leaves) {
                if (used.contains(a.getId()) || !match.test(a)) {
                    continue;
                }
                used.add(a.getId());
                v = v.add(net(close.get(a.getId()), debitSide));
                v2 = v2.add(net(yearOpen.get(a.getId()), debitSide));
            }
            return new BigDecimal[]{v, v2};
        }
    }

    // ---------------------------------------------------------------- 利润表

    /** 利润表：amount 本期金额，amount2 本年累计（不含结转损益凭证） */
    public List<ReportLine> incomeStatement(int period) {
        toYearMonth(period);
        List<FinAccount> leaves = accountService.listAll().stream()
                .filter(a -> a.getLeaf() && AccountCategory.PROFIT.name().equals(a.getCategory())).toList();
        Map<Long, AccountSum> month = toMap(entryMapper.sumByAccount(period, period, true));
        Map<Long, AccountSum> year = toMap(entryMapper.sumByAccount(period / 100 * 100 + 1, period, true));
        Income in = new Income(leaves, month, year);
        BigDecimal[] revenue = in.line("6001", "6051");
        BigDecimal[] cost = in.line("6401", "6402");
        BigDecimal[] tax = in.line("6403");
        BigDecimal[] selling = in.line("6601");
        BigDecimal[] admin = in.line("6602");
        BigDecimal[] finance = in.line("6603");
        BigDecimal[] nonOpIncome = in.line("6301");
        BigDecimal[] nonOpExpense = in.line("6711");
        BigDecimal[] incomeTax = in.line("6801");
        // 未匹配的损益科目：贷方科目计入营业外收入，借方科目计入营业外支出
        BigDecimal[] otherIncome = in.rest("CREDIT");
        BigDecimal[] otherExpense = in.rest("DEBIT");
        nonOpIncome = add(nonOpIncome, otherIncome);
        nonOpExpense = add(nonOpExpense, otherExpense);
        BigDecimal[] operating = sub(sub(sub(sub(sub(revenue, cost), tax), selling), admin), finance);
        BigDecimal[] total = sub(add(operating, nonOpIncome), nonOpExpense);
        BigDecimal[] net = sub(total, incomeTax);
        List<ReportLine> lines = new ArrayList<>();
        lines.add(line("一、营业收入", 0, revenue, true));
        lines.add(line("减：营业成本", 1, cost, false));
        lines.add(line("税金及附加", 1, tax, false));
        lines.add(line("销售费用", 1, selling, false));
        lines.add(line("管理费用", 1, admin, false));
        lines.add(line("财务费用", 1, finance, false));
        lines.add(line("二、营业利润", 0, operating, true));
        lines.add(line("加：营业外收入", 1, nonOpIncome, false));
        lines.add(line("减：营业外支出", 1, nonOpExpense, false));
        lines.add(line("三、利润总额", 0, total, true));
        lines.add(line("减：所得税费用", 1, incomeTax, false));
        lines.add(line("四、净利润", 0, net, true));
        return lines;
    }

    private static final class Income {

        private final List<FinAccount> leaves;
        private final Map<Long, AccountSum> month;
        private final Map<Long, AccountSum> year;
        private final java.util.Set<Long> used = new java.util.HashSet<>();

        Income(List<FinAccount> leaves, Map<Long, AccountSum> month, Map<Long, AccountSum> year) {
            this.leaves = leaves;
            this.month = month;
            this.year = year;
        }

        /** 收入类（贷方科目）取贷 - 借，费用类取借 - 贷 */
        BigDecimal[] line(String... prefixes) {
            return collect(a -> List.of(prefixes).stream().anyMatch(p -> a.getCode().startsWith(p)));
        }

        BigDecimal[] rest(String direction) {
            return collect(a -> direction.equals(a.getDirection()));
        }

        private BigDecimal[] collect(Predicate<FinAccount> match) {
            BigDecimal v = BigDecimal.ZERO, v2 = BigDecimal.ZERO;
            for (FinAccount a : leaves) {
                if (used.contains(a.getId()) || !match.test(a)) {
                    continue;
                }
                used.add(a.getId());
                boolean debitSide = "DEBIT".equals(a.getDirection());
                v = v.add(net(month.get(a.getId()), debitSide));
                v2 = v2.add(net(year.get(a.getId()), debitSide));
            }
            return new BigDecimal[]{v, v2};
        }
    }

    // ---------------------------------------------------------------- 工具

    private static ReportLine line(String name, int level, BigDecimal[] v, boolean bold) {
        return new ReportLine(name, level, v[0], v[1], bold);
    }

    private static BigDecimal[] add(BigDecimal[] a, BigDecimal[] b) {
        return new BigDecimal[]{a[0].add(b[0]), a[1].add(b[1])};
    }

    private static BigDecimal[] sub(BigDecimal[] a, BigDecimal[] b) {
        return new BigDecimal[]{a[0].subtract(b[0]), a[1].subtract(b[1])};
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
            return "平";
        }
        return (balance.signum() > 0) == debitSide ? "借" : "贷";
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
            throw new BizException("起始期间不能晚于截止期间");
        }
    }
}
