package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.mapper.FinCashierMapper;
import com.lemon.erp.modules.fin.model.FinConstants;
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
    public List<Map<String, Object>> journal(Long accountId, LocalDate begin, LocalDate end, boolean posted) {
        checkRange(begin, end);
        List<Long> ids = cashAccountIds(accountId);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (ids.isEmpty()) {
            return rows;
        }
        BigDecimal balance = cashierMapper.sumBefore(ids, begin, posted);
        rows.add(line("OPEN", begin, "期初余额", null, null, balance));
        List<Map<String, Object>> entries = cashierMapper.selectJournal(ids, begin, end, posted);
        Map<Long, String> counters = counterAccounts(entries, ids);
        BigDecimal dayDebit = BigDecimal.ZERO;
        BigDecimal dayCredit = BigDecimal.ZERO;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        LocalDate day = null;
        for (Map<String, Object> e : entries) {
            LocalDate date = toDate(e.get("entryDate"));
            if (day != null && !day.equals(date)) {
                rows.add(line("DAY", day, "本日合计", dayDebit, dayCredit, balance));
                dayDebit = BigDecimal.ZERO;
                dayCredit = BigDecimal.ZERO;
            }
            day = date;
            BigDecimal debit = dec(e.get("debit"));
            BigDecimal credit = dec(e.get("credit"));
            balance = balance.add(debit).subtract(credit);
            dayDebit = dayDebit.add(debit);
            dayCredit = dayCredit.add(credit);
            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
            Map<String, Object> row = line("ENTRY", date, (String) e.get("summary"), debit, credit, balance);
            row.put("voucherId", e.get("voucherId"));
            row.put("voucherNo", e.get("voucherNo"));
            row.put("voucherStatus", e.get("voucherStatus"));
            row.put("accountCode", e.get("accountCode"));
            row.put("accountName", e.get("accountName"));
            row.put("counterAccounts", counters.get(((Number) e.get("voucherId")).longValue()));
            rows.add(row);
        }
        if (day != null) {
            rows.add(line("DAY", day, "本日合计", dayDebit, dayCredit, balance));
        }
        rows.add(line("TOTAL", end, "合计", totalDebit, totalCredit, balance));
        return rows;
    }

    /**
     * 资金日报：各现金、银行末级科目在日期范围内的期初、收入、支出、余额和笔数，只列有余额或有发生额的账户
     *
     * @return {rows: [{accountId, code, name, open, debit, credit, close, inCount, outCount}], total: {...}}
     */
    public Map<String, Object> daily(LocalDate begin, LocalDate end, boolean posted) {
        checkRange(begin, end);
        List<FinAccount> accounts = accountService.leafOptions().stream().filter(FinCashierService::isCash).toList();
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> total = new LinkedHashMap<>();
        BigDecimal[] sum = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        long[] counts = {0, 0};
        if (!accounts.isEmpty()) {
            List<Long> ids = accounts.stream().map(FinAccount::getId).toList();
            Map<Long, BigDecimal> open = new HashMap<>();
            cashierMapper.balanceBefore(ids, begin, posted).forEach(r -> open.put(((Number) r.get("accountId")).longValue(), dec(r.get("balance"))));
            Map<Long, Map<String, Object>> sums = cashierMapper.sumByAccount(ids, begin, end, posted).stream()
                    .collect(Collectors.toMap(r -> ((Number) r.get("accountId")).longValue(), r -> r));
            for (FinAccount a : accounts) {
                BigDecimal o = open.getOrDefault(a.getId(), BigDecimal.ZERO);
                Map<String, Object> s = sums.get(a.getId());
                BigDecimal debit = s == null ? BigDecimal.ZERO : dec(s.get("debit"));
                BigDecimal credit = s == null ? BigDecimal.ZERO : dec(s.get("credit"));
                if (o.signum() == 0 && s == null) {
                    continue;
                }
                long in = s == null ? 0 : ((Number) s.get("inCount")).longValue();
                long out = s == null ? 0 : ((Number) s.get("outCount")).longValue();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("accountId", a.getId());
                row.put("code", a.getCode());
                row.put("name", a.getName());
                row.put("open", o);
                row.put("debit", debit);
                row.put("credit", credit);
                row.put("close", o.add(debit).subtract(credit));
                row.put("inCount", in);
                row.put("outCount", out);
                rows.add(row);
                sum[0] = sum[0].add(o);
                sum[1] = sum[1].add(debit);
                sum[2] = sum[2].add(credit);
                sum[3] = sum[3].add(o.add(debit).subtract(credit));
                counts[0] += in;
                counts[1] += out;
            }
        }
        total.put("open", sum[0]);
        total.put("debit", sum[1]);
        total.put("credit", sum[2]);
        total.put("close", sum[3]);
        total.put("inCount", counts[0]);
        total.put("outCount", counts[1]);
        return Map.of("rows", rows, "total", total);
    }

    /** 还没有生成凭证的收付款（已生效）：{RECEIPT: {count, amount}, PAYMENT: {...}} */
    public Map<String, Object> unvouchered() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map<String, Object> r : cashierMapper.selectUnvouchered()) {
            result.put((String) r.get("kind"), Map.of("count", ((Number) r.get("count")).intValue(), "amount", dec(r.get("amount"))));
        }
        return result;
    }

    private List<Long> cashAccountIds(Long accountId) {
        if (accountId == null) {
            return accountService.listAll().stream().filter(FinCashierService::isCash).map(FinAccount::getId).toList();
        }
        FinAccount account = accountService.get(accountId);
        if (!isCash(account)) {
            throw new BizException("请选择库存现金或银行存款科目");
        }
        return accountService.selfAndChildren(account);
    }

    /** 每张凭证的对方科目（现金、银行以外的科目名称，顿号分隔） */
    private Map<Long, String> counterAccounts(List<Map<String, Object>> entries, List<Long> ids) {
        List<Long> voucherIds = entries.stream().map(e -> ((Number) e.get("voucherId")).longValue()).distinct().toList();
        Map<Long, String> result = new HashMap<>();
        for (int i = 0; i < voucherIds.size(); i += 500) {
            List<Long> chunk = voucherIds.subList(i, Math.min(i + 500, voucherIds.size()));
            cashierMapper.selectCounterAccounts(chunk, ids).forEach(r -> result.merge(((Number) r.get("voucherId")).longValue(),
                    (String) r.get("accountName"), (a, b) -> a.equals(b) ? a : a + "、" + b));
        }
        return result;
    }

    private static boolean isCash(FinAccount a) {
        return a.getCode().startsWith(FinConstants.CASH) || a.getCode().startsWith(FinConstants.BANK);
    }

    private static Map<String, Object> line(String type, LocalDate date, String summary, BigDecimal debit, BigDecimal credit, BigDecimal balance) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("type", type);
        row.put("entryDate", date);
        row.put("summary", summary);
        row.put("debit", debit);
        row.put("credit", credit);
        row.put("balance", balance);
        return row;
    }

    private static void checkRange(LocalDate begin, LocalDate end) {
        if (begin == null || end == null) {
            throw new BizException("请选择日期范围");
        }
        if (end.isBefore(begin)) {
            throw new BizException("结束日期不能早于开始日期");
        }
        if (ChronoUnit.DAYS.between(begin, end) >= MAX_DAYS) {
            throw new BizException("日期范围不能超过一年");
        }
    }

    private static LocalDate toDate(Object v) {
        if (v instanceof LocalDate d) {
            return d;
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        return LocalDate.parse(Objects.toString(v).substring(0, 10));
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
