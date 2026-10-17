package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.util.Decimals;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.entity.FinBankStatement;
import com.lemon.erp.modules.fin.mapper.FinBankStatementMapper;
import com.lemon.erp.modules.fin.model.FinConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 银行对账：导入银行对账单，与银行存款科目的凭证分录勾对（金额相同、日期相近自动勾对，也可手工勾对），生成余额调节表
 */
@Service
@RequiredArgsConstructor
public class FinBankService {

    /** 自动勾对允许的日期差（天） */
    private static final int MATCH_DAYS = 5;

    private final FinBankStatementMapper statementMapper;
    private final FinAccountService accountService;

    public record StatementRow(LocalDate txnDate, BigDecimal amount, String summary, String refNo) {
    }

    public List<FinBankStatement> statements(Long accountId, LocalDate begin, LocalDate end) {
        return statementMapper.selectStatements(accountId, begin, end);
    }

    public List<Map<String, Object>> entries(Long accountId, LocalDate begin, LocalDate end) {
        return statementMapper.selectEntries(accountId, begin, end);
    }

    /**
     * 导入对账单：流水号已导入过的跳过
     *
     * @return 导入的行数
     */
    @Transactional
    public int importRows(Long accountId, List<StatementRow> rows) {
        requireBank(accountId);
        String batch = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Set<String> refs = new HashSet<>();
        int count = 0;
        for (StatementRow r : rows) {
            if (r.txnDate() == null || r.amount() == null || r.amount().signum() == 0) {
                throw new BizException("fin.error.eachStatementLineRequiresTransactionDate");
            }
            String ref = r.refNo() == null || r.refNo().isBlank() ? null : r.refNo().trim();
            if (ref != null && (!refs.add(ref) || statementMapper.exists(Wrappers.<FinBankStatement>lambdaQuery()
                    .eq(FinBankStatement::getAccountId, accountId).eq(FinBankStatement::getRefNo, ref)))) {
                continue;
            }
            FinBankStatement s = new FinBankStatement();
            s.setAccountId(accountId);
            s.setTxnDate(r.txnDate());
            s.setAmount(r.amount());
            s.setSummary(r.summary());
            s.setRefNo(ref);
            s.setBatchNo(batch);
            statementMapper.insert(s);
            count++;
        }
        return count;
    }

    /**
     * 自动勾对：未勾对的流水按金额相同、日期相差不超过 5 天、日期最近的凭证分录一一勾对
     *
     * @return 勾对的笔数
     */
    @Transactional
    public int autoMatch(Long accountId, LocalDate begin, LocalDate end) {
        requireBank(accountId);
        List<Map<String, Object>> entries = new ArrayList<>(statementMapper.selectEntries(accountId,
                begin == null ? null : begin.minusDays(MATCH_DAYS), end == null ? null : end.plusDays(MATCH_DAYS)).stream()
                .filter(e -> ((Number) e.get("matched")).intValue() == 0).toList());
        int count = 0;
        for (FinBankStatement s : statementMapper.selectStatements(accountId, begin, end)) {
            if (s.getEntryId() != null) {
                continue;
            }
            Map<String, Object> best = null;
            long bestDays = Long.MAX_VALUE;
            for (Map<String, Object> e : entries) {
                if (new BigDecimal(e.get("amount").toString()).compareTo(s.getAmount()) != 0) {
                    continue;
                }
                long days = Math.abs(ChronoUnit.DAYS.between(s.getTxnDate(), toDate(e.get("voucherDate"))));
                if (days <= MATCH_DAYS && days < bestDays) {
                    best = e;
                    bestDays = days;
                }
            }
            if (best != null) {
                statementMapper.update(Wrappers.<FinBankStatement>lambdaUpdate()
                        .set(FinBankStatement::getEntryId, ((Number) best.get("entryId")).longValue()).eq(FinBankStatement::getId, s.getId()));
                entries.remove(best);
                count++;
            }
        }
        return count;
    }

    /** 手工勾对：分录须是同一银行科目、金额一致且未被勾对 */
    @Transactional
    public void match(Long statementId, Long entryId) {
        FinBankStatement s = get(statementId);
        Map<String, Object> entry = statementMapper.selectEntries(s.getAccountId(), null, null).stream()
                .filter(e -> ((Number) e.get("entryId")).longValue() == entryId).findFirst()
                .orElseThrow(() -> new BizException("fin.error.voucherEntryDoesNotBelongBank"));
        if (((Number) entry.get("matched")).intValue() == 1) {
            throw new BizException("fin.error.voucherEntryHasAlreadyMatched");
        }
        if (new BigDecimal(entry.get("amount").toString()).compareTo(s.getAmount()) != 0) {
            throw new BizException("fin.error.amountsDifferTransactionEntry", s.getAmount().toPlainString(), entry.get("amount"));
        }
        statementMapper.update(Wrappers.<FinBankStatement>lambdaUpdate().set(FinBankStatement::getEntryId, entryId).eq(FinBankStatement::getId, statementId));
    }

    @Transactional
    public void unmatch(Long statementId) {
        get(statementId);
        statementMapper.update(Wrappers.<FinBankStatement>lambdaUpdate().set(FinBankStatement::getEntryId, null).eq(FinBankStatement::getId, statementId));
    }

    @Transactional
    public void delete(List<Long> ids) {
        statementMapper.deleteByIds(ids);
    }

    /**
     * 余额调节表：截至日期的账面余额 + 银行已收企业未收 − 银行已付企业未付 = 银行对账单余额 + 企业已收银行未收 − 企业已付银行未付
     *
     * @param bankBalance 银行对账单余额，为空时按已导入流水合计
     */
    public Map<String, Object> reconcile(Long accountId, LocalDate asOf, BigDecimal bankBalance) {
        requireBank(accountId);
        LocalDate d = asOf == null ? LocalDate.now() : asOf;
        BigDecimal book = statementMapper.sumBookBalance(accountId, d);
        List<FinBankStatement> statements = statementMapper.selectStatements(accountId, null, d);
        BigDecimal bank = bankBalance != null ? bankBalance
                : Decimals.sum(statements, FinBankStatement::getAmount);
        List<FinBankStatement> bankOnly = statements.stream().filter(s -> s.getEntryId() == null).toList();
        List<Map<String, Object>> bookOnly = statementMapper.selectEntries(accountId, null, d).stream()
                .filter(e -> ((Number) e.get("matched")).intValue() == 0).toList();
        BigDecimal bankIn = bankOnly.stream().map(FinBankStatement::getAmount).filter(a -> a.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bankOut = bankOnly.stream().map(FinBankStatement::getAmount).filter(a -> a.signum() < 0).reduce(BigDecimal.ZERO, BigDecimal::add).negate();
        BigDecimal bookIn = bookOnly.stream().map(e -> new BigDecimal(e.get("amount").toString())).filter(a -> a.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bookOut = bookOnly.stream().map(e -> new BigDecimal(e.get("amount").toString())).filter(a -> a.signum() < 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add).negate();
        BigDecimal adjustedBook = book.add(bankIn).subtract(bankOut);
        BigDecimal adjustedBank = bank.add(bookIn).subtract(bookOut);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("asOf", d);
        r.put("bookBalance", book);
        r.put("bankBalance", bank);
        r.put("bankIn", bankIn);
        r.put("bankOut", bankOut);
        r.put("bookIn", bookIn);
        r.put("bookOut", bookOut);
        r.put("adjustedBook", adjustedBook);
        r.put("adjustedBank", adjustedBank);
        r.put("balanced", adjustedBook.compareTo(adjustedBank) == 0);
        r.put("bankOnly", bankOnly);
        r.put("bookOnly", bookOnly);
        return r;
    }

    private FinBankStatement get(Long id) {
        FinBankStatement s = statementMapper.selectById(id);
        if (s == null) {
            throw new BizException("fin.error.bankStatementLineDoesNotExist");
        }
        return s;
    }

    private void requireBank(Long accountId) {
        FinAccount account = accountService.requireCashAccount(accountId);
        if (!account.getCode().startsWith(FinConstants.BANK)) {
            throw new BizException("fin.error.selectBankDepositAccount");
        }
    }

    private static LocalDate toDate(Object v) {
        if (v instanceof LocalDate d) {
            return d;
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        return LocalDate.parse(v.toString().substring(0, 10));
    }
}
