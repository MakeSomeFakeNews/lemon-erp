package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.fin.dto.BankViews;
import com.lemon.erp.modules.fin.entity.FinBankOpeningItem;
import com.lemon.erp.modules.fin.entity.FinBankStatement;
import com.lemon.erp.modules.fin.entity.FinCashAccount;
import com.lemon.erp.modules.fin.mapper.FinBankOpeningItemMapper;
import com.lemon.erp.modules.fin.mapper.FinBankStatementMapper;
import com.lemon.erp.modules.fin.model.BankOpeningSide;
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
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 银行对账：导入银行对账单，与资金账户入账科目的凭证分录勾对（金额相同、日期相近自动勾对，也可手工勾对），生成余额调节表。
 * 账户有启用日期时：账面余额 = 日记账期初 + 启用日期起的凭证，银行余额 = 对账单期初 + 启用日期起的流水；
 * 期初未达账参与勾对（银行已记企业未记的与凭证分录勾对，企业已记银行未记的与对账单流水勾对），未勾对的列入余额调节表
 */
@Service
@RequiredArgsConstructor
public class FinBankService {

    /** 自动勾对允许的日期差（天） */
    private static final int MATCH_DAYS = 5;

    private final FinBankStatementMapper statementMapper;
    private final FinBankOpeningItemMapper openingItemMapper;
    private final FinCashAccountService cashAccountService;

    public record StatementRow(LocalDate txnDate, BigDecimal amount, String summary, String refNo) {
    }

    public List<FinBankStatement> statements(Long accountId, LocalDate begin, LocalDate end) {
        return statementMapper.selectStatements(accountId, begin, end);
    }

    public List<BankViews.BankEntry> entries(Long accountId, LocalDate begin, LocalDate end) {
        return statementMapper.selectEntries(accountId, begin, end);
    }

    /** 账户的期初未达账（含勾对情况） */
    public List<FinBankOpeningItem> openingItems(Long accountId) {
        return cashAccountService.openingItemsByAccount(accountId);
    }

    /**
     * 导入对账单：流水号已导入过的跳过
     *
     * @return 导入的行数
     */
    @Transactional
    public int importRows(Long accountId, List<StatementRow> rows) {
        cashAccountService.requireBank(accountId);
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
     * 自动勾对：未勾对的流水按金额相同、日期相差不超过 5 天、日期最近的凭证分录一一勾对；
     * 期初未达账同样处理（银行已记企业未记的找凭证分录，企业已记银行未记的找流水）
     *
     * @return 勾对的笔数
     */
    @Transactional
    public int autoMatch(Long accountId, LocalDate begin, LocalDate end) {
        FinCashAccount account = cashAccountService.requireBank(accountId);
        LocalDate from = account.getEnableDate();
        List<BankViews.BankEntry> entries = new ArrayList<>(statementMapper.selectEntries(accountId,
                begin == null ? null : begin.minusDays(MATCH_DAYS), end == null ? null : end.plusDays(MATCH_DAYS)).stream()
                .filter(e -> e.getMatched() == 0 && (from == null || !e.getVoucherDate().isBefore(from))).toList());
        List<FinBankStatement> statements = new ArrayList<>(statementMapper.selectStatements(accountId, begin, end).stream()
                .filter(s -> s.getEntryId() == null && s.getOpeningItemId() == null).toList());
        int count = 0;
        for (FinBankStatement s : List.copyOf(statements)) {
            BankViews.BankEntry best = closest(entries, s.getAmount(), s.getTxnDate(), BankViews.BankEntry::getAmount, BankViews.BankEntry::getVoucherDate);
            if (best != null) {
                statementMapper.update(Wrappers.<FinBankStatement>lambdaUpdate()
                        .set(FinBankStatement::getEntryId, best.getEntryId()).eq(FinBankStatement::getId, s.getId()));
                entries.remove(best);
                statements.remove(s);
                count++;
            }
        }
        if (from == null) {
            return count;
        }
        for (FinBankOpeningItem item : openingItemMapper.selectByCashAccount(account.getId())) {
            if (item.getEntryId() != null || item.getStatementId() != null) {
                continue;
            }
            if (item.getSide() == BankOpeningSide.BANK) {
                BankViews.BankEntry best = closest(entries, item.getAmount(), item.getTxnDate(), BankViews.BankEntry::getAmount, BankViews.BankEntry::getVoucherDate);
                if (best != null) {
                    setItemMatch(item.getId(), best.getEntryId(), null);
                    entries.remove(best);
                    count++;
                }
            } else {
                FinBankStatement best = closest(statements.stream().filter(s -> !s.getTxnDate().isBefore(from)).toList(), item.getAmount(),
                        item.getTxnDate(), FinBankStatement::getAmount, FinBankStatement::getTxnDate);
                if (best != null) {
                    setItemMatch(item.getId(), null, best.getId());
                    statements.remove(best);
                    count++;
                }
            }
        }
        return count;
    }

    /** 金额相同、日期相差不超过 5 天中日期最近的一条 */
    private static <T> T closest(List<T> candidates, BigDecimal amount, LocalDate date, Function<T, BigDecimal> amountOf, Function<T, LocalDate> dateOf) {
        T best = null;
        long bestDays = Long.MAX_VALUE;
        for (T c : candidates) {
            if (amountOf.apply(c).compareTo(amount) != 0) {
                continue;
            }
            long days = Math.abs(ChronoUnit.DAYS.between(date, dateOf.apply(c)));
            if (days <= MATCH_DAYS && days < bestDays) {
                best = c;
                bestDays = days;
            }
        }
        return best;
    }

    /** 手工勾对：分录须是同一银行科目、金额一致且未被勾对 */
    @Transactional
    public void match(Long statementId, Long entryId) {
        FinBankStatement s = get(statementId);
        if (s.getEntryId() != null || openingItemMapper.exists(Wrappers.<FinBankOpeningItem>lambdaQuery().eq(FinBankOpeningItem::getStatementId, statementId))) {
            throw new BizException("fin.error.statementAlreadyMatched");
        }
        BankViews.BankEntry entry = unmatchedEntry(s.getAccountId(), entryId);
        if (entry.getAmount().compareTo(s.getAmount()) != 0) {
            throw new BizException("fin.error.amountsDifferTransactionEntry", s.getAmount().toPlainString(), entry.getAmount());
        }
        statementMapper.update(Wrappers.<FinBankStatement>lambdaUpdate().set(FinBankStatement::getEntryId, entryId).eq(FinBankStatement::getId, statementId));
    }

    @Transactional
    public void unmatch(Long statementId) {
        get(statementId);
        statementMapper.update(Wrappers.<FinBankStatement>lambdaUpdate().set(FinBankStatement::getEntryId, null).eq(FinBankStatement::getId, statementId));
        openingItemMapper.update(Wrappers.<FinBankOpeningItem>lambdaUpdate().set(FinBankOpeningItem::getStatementId, null)
                .eq(FinBankOpeningItem::getStatementId, statementId));
    }

    /**
     * 手工勾对期初未达账：银行已记企业未记的与凭证分录（启用日期起）勾对，企业已记银行未记的与对账单流水（启用日期起）勾对，金额须一致
     *
     * @param targetId 凭证分录 ID 或对账单流水 ID（按未达账的一方）
     */
    @Transactional
    public void matchOpening(Long itemId, Long targetId) {
        FinBankOpeningItem item = getItem(itemId);
        if (item.getEntryId() != null || item.getStatementId() != null) {
            throw new BizException("fin.error.openingItemMatched");
        }
        FinCashAccount account = cashAccountService.get(item.getCashAccountId());
        if (item.getSide() == BankOpeningSide.BANK) {
            BankViews.BankEntry entry = unmatchedEntry(account.getAccountId(), targetId);
            if (account.getEnableDate() != null && entry.getVoucherDate().isBefore(account.getEnableDate())) {
                throw new BizException("fin.error.openingItemMatchBeforeEnable", account.getEnableDate().toString());
            }
            if (entry.getAmount().compareTo(item.getAmount()) != 0) {
                throw new BizException("fin.error.amountsDifferTransactionEntry", item.getAmount().toPlainString(), entry.getAmount());
            }
            setItemMatch(itemId, targetId, null);
            return;
        }
        FinBankStatement s = get(targetId);
        if (!s.getAccountId().equals(account.getAccountId())) {
            throw new BizException("fin.error.bankStatementLineDoesNotExist");
        }
        if (s.getEntryId() != null || openingItemMapper.exists(Wrappers.<FinBankOpeningItem>lambdaQuery().eq(FinBankOpeningItem::getStatementId, targetId))) {
            throw new BizException("fin.error.statementAlreadyMatched");
        }
        if (account.getEnableDate() != null && s.getTxnDate().isBefore(account.getEnableDate())) {
            throw new BizException("fin.error.openingItemMatchBeforeEnable", account.getEnableDate().toString());
        }
        if (s.getAmount().compareTo(item.getAmount()) != 0) {
            throw new BizException("fin.error.amountsDifferTransactionEntry", s.getAmount().toPlainString(), item.getAmount());
        }
        setItemMatch(itemId, null, targetId);
    }

    @Transactional
    public void unmatchOpening(Long itemId) {
        getItem(itemId);
        setItemMatch(itemId, null, null);
    }

    @Transactional
    public void delete(List<Long> ids) {
        openingItemMapper.update(Wrappers.<FinBankOpeningItem>lambdaUpdate().set(FinBankOpeningItem::getStatementId, null)
                .in(FinBankOpeningItem::getStatementId, ids));
        statementMapper.deleteByIds(ids);
    }

    /**
     * 余额调节表：截至日期的账面余额 + 银行已收企业未收 − 银行已付企业未付 = 银行对账单余额 + 企业已收银行未收 − 企业已付银行未付。
     * 账户有启用日期时，账面余额 = 日记账期初 + 启用日期起的凭证，银行余额 = 对账单期初 + 启用日期起的流水，未勾对的期初未达账计入
     *
     * @param bankBalance 银行对账单余额，为空时按对账单期初 + 已导入流水合计
     */
    public BankViews.Reconciliation reconcile(Long accountId, LocalDate asOf, BigDecimal bankBalance) {
        FinCashAccount account = cashAccountService.requireBank(accountId);
        LocalDate from = account.getEnableDate();
        LocalDate d = asOf == null ? LocalDate.now() : asOf;
        BigDecimal book = statementMapper.sumBookBalance(accountId, d, from);
        List<FinBankStatement> statements = statementMapper.selectStatements(accountId, from, d);
        if (from != null) {
            book = book.add(Decimals.nz(account.getJournalOpening()));
        }
        BigDecimal bank = bankBalance != null ? bankBalance
                : Decimals.sum(statements, FinBankStatement::getAmount).add(from == null ? BigDecimal.ZERO : Decimals.nz(account.getStatementOpening()));
        List<FinBankStatement> bankOnly = new ArrayList<>(statements.stream().filter(s -> s.getEntryId() == null && s.getOpeningItemId() == null).toList());
        List<BankViews.BankEntry> bookOnly = new ArrayList<>(statementMapper.selectEntries(accountId, from, d).stream()
                .filter(e -> e.getMatched() == 0).toList());
        if (from != null) {
            for (FinBankOpeningItem item : openingItemMapper.selectByCashAccount(account.getId())) {
                if (item.getSide() == BankOpeningSide.BANK && item.getEntryId() == null) {
                    bankOnly.add(asStatement(item));
                } else if (item.getSide() == BankOpeningSide.BOOK && item.getStatementId() == null) {
                    bookOnly.add(asEntry(item));
                }
            }
        }
        BigDecimal bankIn = bankOnly.stream().map(FinBankStatement::getAmount).filter(a -> a.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bankOut = bankOnly.stream().map(FinBankStatement::getAmount).filter(a -> a.signum() < 0).reduce(BigDecimal.ZERO, BigDecimal::add).negate();
        BigDecimal bookIn = bookOnly.stream().map(BankViews.BankEntry::getAmount).filter(a -> a.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bookOut = bookOnly.stream().map(BankViews.BankEntry::getAmount).filter(a -> a.signum() < 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add).negate();
        BigDecimal adjustedBook = book.add(bankIn).subtract(bankOut);
        BigDecimal adjustedBank = bank.add(bookIn).subtract(bookOut);
        return new BankViews.Reconciliation(d, book, bank, bankIn, bankOut, bookIn, bookOut, adjustedBook, adjustedBank,
                adjustedBook.compareTo(adjustedBank) == 0, bankOnly, bookOnly);
    }

    private static FinBankStatement asStatement(FinBankOpeningItem item) {
        FinBankStatement s = new FinBankStatement();
        s.setId(item.getId());
        s.setTxnDate(item.getTxnDate());
        s.setAmount(item.getAmount());
        s.setSummary(item.getSummary());
        s.setRefNo(item.getRefNo());
        s.setOpening(true);
        return s;
    }

    private static BankViews.BankEntry asEntry(FinBankOpeningItem item) {
        BankViews.BankEntry e = new BankViews.BankEntry();
        e.setOpeningItemId(item.getId());
        e.setVoucherDate(item.getTxnDate());
        e.setSummary(item.getSummary());
        e.setAmount(item.getAmount());
        e.setMatched(0);
        return e;
    }

    private BankViews.BankEntry unmatchedEntry(Long accountId, Long entryId) {
        BankViews.BankEntry entry = statementMapper.selectEntries(accountId, null, null).stream()
                .filter(e -> e.getEntryId().equals(entryId)).findFirst()
                .orElseThrow(() -> new BizException("fin.error.voucherEntryDoesNotBelongBank"));
        if (entry.getMatched() == 1) {
            throw new BizException("fin.error.voucherEntryHasAlreadyMatched");
        }
        return entry;
    }

    private void setItemMatch(Long itemId, Long entryId, Long statementId) {
        openingItemMapper.update(Wrappers.<FinBankOpeningItem>lambdaUpdate()
                .set(FinBankOpeningItem::getEntryId, entryId).set(FinBankOpeningItem::getStatementId, statementId)
                .eq(FinBankOpeningItem::getId, itemId));
    }

    private FinBankStatement get(Long id) {
        FinBankStatement s = statementMapper.selectById(id);
        if (s == null) {
            throw new BizException("fin.error.bankStatementLineDoesNotExist");
        }
        return s;
    }

    private FinBankOpeningItem getItem(Long id) {
        FinBankOpeningItem item = openingItemMapper.selectById(id);
        if (item == null) {
            throw new BizException("fin.error.openingItemNotFound");
        }
        return item;
    }
}
