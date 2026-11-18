package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.fin.dto.CashAccountRequests;
import com.lemon.erp.modules.fin.dto.CashierViews;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.entity.FinBankOpeningItem;
import com.lemon.erp.modules.fin.entity.FinCashAccount;
import com.lemon.erp.modules.fin.mapper.FinBankOpeningItemMapper;
import com.lemon.erp.modules.fin.mapper.FinCashAccountMapper;
import com.lemon.erp.modules.fin.mapper.FinCashierMapper;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.lemon.erp.modules.fin.model.BankOpeningSide;
import com.lemon.erp.modules.fin.model.CashAccountType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 资金账户（出纳账户）：建账户、绑入账科目、录期初、启用 / 停用。
 * <ul>
 *   <li>入账科目：「指定科目」（凭证科目设置中的库存现金、银行存款、其他货币资金，{@link AccountRole#scope()}）下的末级科目，
 *       现金账户在现金科目下，其他类型在银行科目或其他货币资金科目下；不能启用辅助核算；一个科目只对应一个账户。
 *       单据中保存的仍是入账科目 ID，账户与科目一一对应，因此入账科目被使用后不能再修改</li>
 *   <li>期初：启用日期为空表示从建账开始（日记账期初取科目期初余额）；有启用日期时日记账从该日起按「日记账期初」计算，
 *       银行类账户另录对账单期初和期初未达账，调平后才能启用</li>
 *   <li>状态：停用的账户不出现在单据的账户下拉中，也不能再用于新的收付；停用后才能修改类型、入账科目、启用日期和期初</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FinCashAccountService {

    private final FinCashAccountMapper cashAccountMapper;
    private final FinBankOpeningItemMapper openingItemMapper;
    private final FinCashierMapper cashierMapper;
    private final FinAccountService accountService;

    public List<FinCashAccount> list() {
        return cashAccountMapper.selectViewList(null);
    }

    /** 单据账户下拉：全部资金账户，启用且入账科目可用的才能选 */
    public List<CashAccountRequests.Option> options() {
        Map<Long, FinAccount> leaves = accountService.leafOptions().stream()
                .collect(Collectors.toMap(FinAccount::getId, Function.identity()));
        List<String> scopes = accountService.cashCodes();
        return list().stream().map(c -> {
            FinAccount a = leaves.get(c.getAccountId());
            boolean usable = c.getStatus() == EnableStatus.ENABLED && a != null && FinAccountService.inScope(a.getCode(), scopes);
            return new CashAccountRequests.Option(c.getId(), c.getName(), c.getType(), c.getAccountId(), c.getAccountCode(),
                    c.getAccountName(), c.getEnableDate(), c.getStatus(), usable);
        }).toList();
    }

    public FinCashAccount get(Long id) {
        List<FinCashAccount> list = id == null ? List.of() : cashAccountMapper.selectViewList(id);
        if (list.isEmpty()) {
            throw new BizException("fin.error.cashAccountNotFound");
        }
        return list.get(0);
    }

    /** 入账科目对应的资金账户 */
    public FinCashAccount byAccountId(Long accountId) {
        FinCashAccount c = accountId == null ? null : accountService.boundCashAccount(accountId);
        if (c == null) {
            throw new BizException("fin.error.selectCashAccount");
        }
        return c;
    }

    /** @return 新账户 ID */
    @Transactional
    public Long create(CashAccountRequests.Save req) {
        FinCashAccount c = new FinCashAccount();
        fill(c, req);
        validateSubject(null, c.getType(), c.getAccountId());
        c.setStatus(EnableStatus.DISABLED);
        insertOrUpdate(c, true);
        if (Boolean.TRUE.equals(req.enable())) {
            enable(c.getId());
        }
        return c.getId();
    }

    /** 启用中的账户只能修改名称、开户行、账号、排序、备注；入账科目被使用后不能修改 */
    public void update(Long id, CashAccountRequests.Save req) {
        FinCashAccount old = get(id);
        FinCashAccount c = new FinCashAccount();
        fill(c, req);
        c.setId(id);
        if (old.getStatus() == EnableStatus.ENABLED) {
            if (c.getType() != old.getType() || !c.getAccountId().equals(old.getAccountId())
                    || !Objects.equals(c.getEnableDate(), old.getEnableDate())
                    || c.getJournalOpening().compareTo(old.getJournalOpening()) != 0
                    || c.getStatementOpening().compareTo(old.getStatementOpening()) != 0) {
                throw new BizException("fin.error.cashAccountEnabledLocked", old.getName());
            }
        } else {
            if (!c.getAccountId().equals(old.getAccountId()) && cashAccountMapper.subjectUsed(old.getAccountId())) {
                throw new BizException("fin.error.cashAccountSubjectUsed", old.getName(), old.getAccountCode(), old.getAccountName());
            }
            if (old.getOpeningItemCount() > 0 && (c.getType() == CashAccountType.CASH || c.getEnableDate() == null)) {
                throw new BizException("fin.error.cashAccountHasOpeningItems", old.getName());
            }
            validateSubject(id, c.getType(), c.getAccountId());
        }
        insertOrUpdate(c, false);
        // updateById 不更新 null 字段：清空启用日期、开户行、账号、备注
        cashAccountMapper.update(Wrappers.<FinCashAccount>lambdaUpdate()
                .set(FinCashAccount::getEnableDate, c.getEnableDate())
                .set(FinCashAccount::getBankName, c.getBankName())
                .set(FinCashAccount::getBankAccountNo, c.getBankAccountNo())
                .set(FinCashAccount::getRemark, c.getRemark())
                .eq(FinCashAccount::getId, id));
    }

    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            FinCashAccount c = get(id);
            if (c.getUsed()) {
                throw new BizException("fin.error.cashAccountUsedCannotDelete", c.getName());
            }
        }
        openingItemMapper.delete(Wrappers.<FinBankOpeningItem>lambdaQuery().in(FinBankOpeningItem::getCashAccountId, ids));
        cashAccountMapper.deleteByIds(ids);
    }

    /** 启用：入账科目仍然可用；银行类账户有启用日期时期初须平衡 */
    public void enable(Long id) {
        FinCashAccount c = get(id);
        validateSubject(id, c.getType(), c.getAccountId());
        if (c.getEnableDate() != null) {
            List<FinBankOpeningItem> items = openingItemMapper.selectByCashAccount(id);
            if (items.stream().anyMatch(i -> !i.getTxnDate().isBefore(c.getEnableDate()))) {
                throw new BizException("fin.error.openingItemDateMustBeforeEnable", c.getEnableDate().toString());
            }
            CashAccountRequests.OpeningCheck check = openingCheck(c, items);
            if (!check.balanced()) {
                throw new BizException("fin.error.cashAccountOpeningUnbalanced", c.getName(),
                        check.adjustedBook().toPlainString(), check.adjustedBank().toPlainString());
            }
        }
        changeStatus(id, EnableStatus.ENABLED);
    }

    public void disable(Long id) {
        get(id);
        changeStatus(id, EnableStatus.DISABLED);
    }

    // ---------------------------------------------------------------- 期初

    public List<FinBankOpeningItem> openingItems(Long id) {
        get(id);
        return openingItemMapper.selectByCashAccount(id);
    }

    /** 银行对账使用：入账科目对应账户的期初未达账 */
    public List<FinBankOpeningItem> openingItemsByAccount(Long accountId) {
        return openingItemMapper.selectByCashAccount(byAccountId(accountId).getId());
    }

    /** 新增期初未达账：停用（未启用）的银行类账户，须先填写启用日期，日期在启用日期之前 */
    public Long addOpeningItem(Long id, CashAccountRequests.OpeningItemSave req) {
        FinCashAccount c = requireEditableOpening(id);
        if (c.getType() == CashAccountType.CASH) {
            throw new BizException("fin.error.cashAccountNoStatement");
        }
        if (c.getEnableDate() == null) {
            throw new BizException("fin.error.cashAccountEnableDateRequired");
        }
        if (!req.txnDate().isBefore(c.getEnableDate())) {
            throw new BizException("fin.error.openingItemDateMustBeforeEnable", c.getEnableDate().toString());
        }
        if (req.amount().signum() == 0) {
            throw new BizException("fin.error.openingItemAmountZero");
        }
        FinBankOpeningItem item = new FinBankOpeningItem();
        item.setCashAccountId(id);
        item.setSide(req.side());
        item.setTxnDate(req.txnDate());
        item.setAmount(req.amount());
        item.setSummary(StringUtils.hasText(req.summary()) ? req.summary().trim() : null);
        item.setRefNo(StringUtils.hasText(req.refNo()) ? req.refNo().trim() : null);
        openingItemMapper.insert(item);
        return item.getId();
    }

    public void deleteOpeningItem(Long itemId) {
        FinBankOpeningItem item = openingItemMapper.selectById(itemId);
        if (item == null) {
            throw new BizException("fin.error.openingItemNotFound");
        }
        requireEditableOpening(item.getCashAccountId());
        if (item.getEntryId() != null || item.getStatementId() != null) {
            throw new BizException("fin.error.openingItemMatched");
        }
        openingItemMapper.deleteById(itemId);
    }

    /** 期初平衡检查，附入账科目在启用日期之前的账面余额供参考 */
    public CashAccountRequests.OpeningCheck openingCheck(Long id) {
        FinCashAccount c = get(id);
        return openingCheck(c, openingItemMapper.selectByCashAccount(id));
    }

    /** 科目在日期之前的账面余额：科目期初 + 凭证（含未审核、未记账），录入日记账期初时参考 */
    public BigDecimal ledgerBalance(Long accountId, LocalDate date) {
        return cashierMapper.sumBefore(List.of(new CashierViews.Scope(accountId, null)), List.of(accountId), date, false);
    }

    private CashAccountRequests.OpeningCheck openingCheck(FinCashAccount c, List<FinBankOpeningItem> items) {
        BigDecimal journal = c.getEnableDate() == null ? BigDecimal.ZERO : Decimals.nz(c.getJournalOpening());
        BigDecimal statement = c.getEnableDate() == null || c.getType() == CashAccountType.CASH ? journal : Decimals.nz(c.getStatementOpening());
        BigDecimal bankIn = sum(items, BankOpeningSide.BANK, 1);
        BigDecimal bankOut = sum(items, BankOpeningSide.BANK, -1);
        BigDecimal bookIn = sum(items, BankOpeningSide.BOOK, 1);
        BigDecimal bookOut = sum(items, BankOpeningSide.BOOK, -1);
        BigDecimal adjustedBook = journal.add(bankIn).subtract(bankOut);
        BigDecimal adjustedBank = statement.add(bookIn).subtract(bookOut);
        BigDecimal ledger = c.getEnableDate() == null ? null : ledgerBalance(c.getAccountId(), c.getEnableDate());
        return new CashAccountRequests.OpeningCheck(journal, statement, bankIn, bankOut, bookIn, bookOut, adjustedBook, adjustedBank,
                adjustedBook.compareTo(adjustedBank) == 0, ledger);
    }

    /** 某一方、某个方向（1 收入 / -1 支出）的未达账合计（正数） */
    private static BigDecimal sum(List<FinBankOpeningItem> items, BankOpeningSide side, int sign) {
        return items.stream().filter(i -> i.getSide() == side && i.getAmount().signum() == sign)
                .map(i -> i.getAmount().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private FinCashAccount requireEditableOpening(Long id) {
        FinCashAccount c = get(id);
        if (c.getStatus() == EnableStatus.ENABLED) {
            throw new BizException("fin.error.cashAccountEnabledLocked", c.getName());
        }
        return c;
    }

    // ---------------------------------------------------------------- 内部

    /** 入账科目：启用的末级科目、不启用辅助核算、在账户类型对应的指定科目下、没有被其他账户使用 */
    private void validateSubject(Long selfId, CashAccountType type, Long accountId) {
        FinAccount a = accountService.requireLeaves(List.of(accountId)).get(accountId);
        String cash = accountService.roleCode(AccountRole.CASH);
        List<String> scopes = type == CashAccountType.CASH ? List.of(cash)
                : List.of(accountService.roleCode(AccountRole.BANK), accountService.roleCode(AccountRole.OTHER_FUNDS));
        if (!FinAccountService.inScope(a.getCode(), scopes)) {
            throw new BizException(type == CashAccountType.CASH ? "fin.error.cashAccountSubjectNotCash" : "fin.error.cashAccountSubjectNotBank",
                    a.getCode(), a.getName(), String.join(Messages.get("fin.text.listSeparator"), scopes));
        }
        if (StringUtils.hasText(a.getAuxItems())) {
            throw new BizException("fin.error.cashAccountSubjectHasAux", a.getCode(), a.getName());
        }
        FinCashAccount other = accountService.boundCashAccount(accountId);
        if (other != null && !other.getId().equals(selfId)) {
            throw new BizException("fin.error.cashAccountSubjectTaken", a.getCode(), a.getName(), other.getName());
        }
    }

    private static void fill(FinCashAccount c, CashAccountRequests.Save req) {
        c.setName(req.name().trim());
        c.setType(req.type());
        c.setAccountId(req.accountId());
        c.setBankName(StringUtils.hasText(req.bankName()) ? req.bankName().trim() : null);
        c.setBankAccountNo(StringUtils.hasText(req.bankAccountNo()) ? req.bankAccountNo().trim() : null);
        c.setEnableDate(req.enableDate());
        // 没有启用日期时期初取科目期初余额，不单独录入
        boolean opening = req.enableDate() != null;
        c.setJournalOpening(opening ? Decimals.nz(req.journalOpening()) : BigDecimal.ZERO);
        c.setStatementOpening(opening && req.type() != CashAccountType.CASH ? Decimals.nz(req.statementOpening()) : BigDecimal.ZERO);
        c.setSort(req.sort() == null ? 0 : req.sort());
        c.setRemark(StringUtils.hasText(req.remark()) ? req.remark().trim() : null);
    }

    private void insertOrUpdate(FinCashAccount c, boolean insert) {
        try {
            if (insert) {
                cashAccountMapper.insert(c);
            } else {
                cashAccountMapper.updateById(c);
            }
        } catch (DuplicateKeyException e) {
            throw new BizException("fin.error.cashAccountNameExists", c.getName());
        }
    }

    private void changeStatus(Long id, EnableStatus status) {
        cashAccountMapper.update(Wrappers.<FinCashAccount>lambdaUpdate().set(FinCashAccount::getStatus, status).eq(FinCashAccount::getId, id));
    }

    /** 银行对账：入账科目对应的非现金账户（停用的也可以查看、勾对） */
    public FinCashAccount requireBank(Long accountId) {
        FinCashAccount c = byAccountId(accountId);
        if (c.getType() == CashAccountType.CASH) {
            throw new BizException("fin.error.selectBankDepositAccount");
        }
        return c;
    }
}
