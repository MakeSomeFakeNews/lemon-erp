package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.fin.dto.AccountRequest;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.entity.FinAccountRule;
import com.lemon.erp.modules.fin.entity.FinAsset;
import com.lemon.erp.modules.fin.entity.FinBudget;
import com.lemon.erp.modules.fin.entity.FinSettlement;
import com.lemon.erp.modules.fin.entity.FinVoucherEntry;
import com.lemon.erp.modules.fin.mapper.FinAccountMapper;
import com.lemon.erp.modules.fin.mapper.FinAccountRuleMapper;
import com.lemon.erp.modules.fin.mapper.FinAssetMapper;
import com.lemon.erp.modules.fin.mapper.FinBudgetMapper;
import com.lemon.erp.modules.fin.mapper.FinSettlementMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherEntryMapper;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.lemon.erp.modules.fin.model.AuxItem;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 会计科目：一级科目 4 位编码，下级科目在上级编码后加 2 位，类别、余额方向继承上级。
 * 凭证只能使用末级科目；已使用的科目不能删除、不能修改编码和方向，也不能再增加下级（否则已有分录挂在非末级科目上）
 */
@Service
@RequiredArgsConstructor
public class FinAccountService {

    /** 科目范围用途：设置的可以是上级科目，不妨碍增加下级 */
    private static final List<AccountRole> SCOPE_ROLES = Arrays.stream(AccountRole.values()).filter(AccountRole::scope).toList();

    private final FinAccountMapper accountMapper;
    private final FinVoucherEntryMapper entryMapper;
    private final FinSettlementMapper settlementMapper;
    private final FinAssetMapper assetMapper;
    private final FinBudgetMapper budgetMapper;
    private final FinAccountRuleMapper ruleMapper;

    /** 科目树，按编码排序 */
    public List<FinAccount> tree(String keyword) {
        List<FinAccount> all = listAll();
        if (StringUtils.hasText(keyword)) {
            // 搜索时平铺返回匹配的科目
            return all.stream().filter(a -> a.getCode().startsWith(keyword) || a.getName().contains(keyword)).toList();
        }
        Map<Long, FinAccount> byId = all.stream().collect(Collectors.toMap(FinAccount::getId, Function.identity()));
        List<FinAccount> roots = new ArrayList<>();
        for (FinAccount a : all) {
            FinAccount parent = byId.get(a.getParentId());
            if (parent == null) {
                roots.add(a);
            } else {
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(a);
            }
        }
        return roots;
    }

    /** 全部科目（含停用），按编码排序，标记是否末级 */
    public List<FinAccount> listAll() {
        List<FinAccount> all = accountMapper.selectList(Wrappers.<FinAccount>lambdaQuery().orderByAsc(FinAccount::getCode));
        Set<Long> parents = all.stream().map(FinAccount::getParentId).collect(Collectors.toSet());
        all.forEach(a -> a.setLeaf(!parents.contains(a.getId())));
        return all;
    }

    /** 启用的末级科目，凭证录入、结算账户、折旧科目选择 */
    public List<FinAccount> leafOptions() {
        return listAll().stream().filter(a -> a.getLeaf() && a.getStatus() == EnableStatus.ENABLED).toList();
    }

    public FinAccount get(Long id) {
        FinAccount account = id == null ? null : accountMapper.selectById(id);
        if (account == null) {
            throw new BizException("fin.error.accountDoesNotExist");
        }
        return account;
    }

    public void create(AccountRequest req) {
        FinAccount account = new FinAccount();
        fill(account, req, null);
        account.setStatus(EnableStatus.ENABLED);
        try {
            accountMapper.insert(account);
        } catch (DuplicateKeyException e) {
            throw new BizException("fin.error.accountCodeAlreadyExists");
        }
    }

    /** 已使用的科目只能修改名称、备注、辅助核算 */
    @Transactional
    public void update(Long id, AccountRequest req) {
        FinAccount old = get(id);
        if (used(id) || hasChildren(id)) {
            if (!old.getCode().equals(req.code()) || (req.direction() != null && old.getDirection() != req.direction())) {
                throw new BizException("fin.error.accountUseHasSubAccountsOnly");
            }
            accountMapper.update(Wrappers.<FinAccount>lambdaUpdate()
                    .set(FinAccount::getName, req.name().trim())
                    .set(FinAccount::getRemark, req.remark())
                    .set(FinAccount::getAuxItems, AuxItem.join(req.auxItems()))
                    .eq(FinAccount::getId, id));
            return;
        }
        FinAccount account = new FinAccount();
        fill(account, req, id);
        account.setId(id);
        try {
            accountMapper.updateById(account);
        } catch (DuplicateKeyException e) {
            throw new BizException("fin.error.accountCodeAlreadyExists");
        }
        // updateById 不更新 null 字段：取消全部辅助核算时单独清空
        if (account.getAuxItems() == null) {
            accountMapper.update(Wrappers.<FinAccount>lambdaUpdate().set(FinAccount::getAuxItems, null).eq(FinAccount::getId, id));
        }
    }

    public void changeStatus(Long id, EnableStatus status) {
        get(id);
        accountMapper.update(Wrappers.<FinAccount>lambdaUpdate().set(FinAccount::getStatus, status).eq(FinAccount::getId, id));
    }

    public void delete(List<Long> ids) {
        for (FinAccount a : accountMapper.selectByIds(ids)) {
            if (hasChildren(a.getId())) {
                throw new BizException("fin.error.accountHasSubAccountsCannotDeleted", a.getCode(), a.getName());
            }
            if (used(a.getId())) {
                throw new BizException("fin.error.accountUseCannotDeletedDisabled", a.getCode(), a.getName());
            }
        }
        accountMapper.deleteByIds(ids);
    }

    // ---------------------------------------------------------------- 供其他服务使用

    /** 按编码取启用的末级科目，业务自动生成凭证使用 */
    public FinAccount leafByCode(String code) {
        FinAccount account = accountMapper.selectOne(Wrappers.<FinAccount>lambdaQuery().eq(FinAccount::getCode, code));
        if (account == null) {
            throw new BizException("fin.error.accountMissingAddChartAccounts", code);
        }
        if (hasChildren(account.getId())) {
            throw new BizException("fin.error.accountHasSubAccountsGeneratedVouchers", code, account.getName());
        }
        if (account.getStatus() != EnableStatus.ENABLED) {
            throw new BizException("fin.error.accountDisabled", code, account.getName());
        }
        return account;
    }

    /** 校验科目存在、启用且为末级，返回 ID → 科目 */
    public Map<Long, FinAccount> requireLeaves(Collection<Long> ids) {
        Map<Long, FinAccount> map = accountMapper.selectByIds(new HashSet<>(ids)).stream()
                .collect(Collectors.toMap(FinAccount::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Set<Long> parents = accountMapper.selectList(Wrappers.<FinAccount>lambdaQuery()
                        .select(FinAccount::getParentId).in(FinAccount::getParentId, ids))
                .stream().map(FinAccount::getParentId).collect(Collectors.toSet());
        for (Long id : ids) {
            FinAccount a = map.get(id);
            if (a == null) {
                throw new BizException("fin.error.accountDoesNotExist2", id);
            }
            if (parents.contains(id)) {
                throw new BizException("fin.error.accountNotLeafAccount", a.getCode(), a.getName());
            }
            if (a.getStatus() != EnableStatus.ENABLED) {
                throw new BizException("fin.error.accountDisabled2", a.getCode(), a.getName());
            }
        }
        return map;
    }

    /** 结算科目：库存现金、银行存款及其下级的末级科目（收付款、发放工资） */
    public FinAccount requireCashAccount(Long accountId) {
        FinAccount account = requireLeaves(List.of(accountId)).get(accountId);
        if (!inScope(account.getCode(), cashCodes())) {
            throw new BizException("fin.error.settlementAccountMustCashBankDeposit");
        }
        return account;
    }

    /**
     * 用途当前使用的科目编码（不校验末级、启用）：「凭证科目设置」中设置的默认科目，未设置时取预置编码。
     * 科目范围用途（库存现金、银行存款）及按编码前缀归类的场景（如现金流量表识别进项、销项税额）使用
     */
    public String roleCode(AccountRole role) {
        FinAccountRule rule = ruleMapper.selectOne(Wrappers.<FinAccountRule>lambdaQuery()
                .eq(FinAccountRule::getRole, role).eq(FinAccountRule::getDimValue, ""));
        FinAccount account = rule == null ? null : accountMapper.selectById(rule.getAccountId());
        return account == null ? role.defaultCode() : account.getCode();
    }

    /** 货币资金科目（库存现金、银行存款）的编码，其下级科目都是货币资金 */
    public List<String> cashCodes() {
        return List.of(roleCode(AccountRole.CASH), roleCode(AccountRole.BANK));
    }

    /** 科目范围用途当前使用的科目编码，供前端限定结算账户等可选科目 */
    public Map<String, String> scopeCodes() {
        Map<String, String> codes = new LinkedHashMap<>();
        for (AccountRole role : AccountRole.values()) {
            if (role.scope()) {
                codes.put(role.name(), roleCode(role));
            }
        }
        return codes;
    }

    /** 科目编码是否在其中某个科目之下（含本身） */
    public static boolean inScope(String code, Collection<String> prefixes) {
        return prefixes.stream().anyMatch(code::startsWith);
    }

    /** 科目及其全部下级的 ID（编码前缀匹配） */
    List<Long> selfAndChildren(FinAccount account) {
        return accountMapper.selectList(Wrappers.<FinAccount>lambdaQuery()
                        .select(FinAccount::getId).likeRight(FinAccount::getCode, account.getCode()))
                .stream().map(FinAccount::getId).toList();
    }

    // ---------------------------------------------------------------- 内部

    private void fill(FinAccount account, AccountRequest req, Long selfId) {
        String code = req.code().trim();
        account.setCode(code);
        account.setName(req.name().trim());
        account.setRemark(req.remark());
        account.setAuxItems(AuxItem.join(req.auxItems()));
        if (req.parentId() == null || req.parentId() == 0) {
            if (code.length() != 4) {
                throw new BizException("fin.error.levelAccountCodeMustDigits");
            }
            if (req.category() == null) {
                throw new BizException("fin.error.selectAccountCategory");
            }
            if (req.direction() == null) {
                throw new BizException("fin.error.selectBalanceDirection");
            }
            account.setParentId(0L);
            account.setCategory(req.category());
            account.setDirection(req.direction());
            return;
        }
        FinAccount parent = get(req.parentId());
        if (parent.getId().equals(selfId)) {
            throw new BizException("fin.error.accountCannotOwnParent");
        }
        if (!code.startsWith(parent.getCode()) || code.length() != parent.getCode().length() + 2) {
            throw new BizException("fin.error.subAccountCodeParentCodeFollowed", parent.getCode());
        }
        if (selfId == null && usedInBooks(parent.getId())) {
            throw new BizException("fin.error.parentAccountUseSubAccountsCannot", parent.getName());
        }
        account.setParentId(parent.getId());
        account.setCategory(parent.getCategory());
        account.setDirection(req.direction() != null ? req.direction() : parent.getDirection());
    }

    private boolean hasChildren(Long id) {
        return accountMapper.exists(Wrappers.<FinAccount>lambdaQuery().eq(FinAccount::getParentId, id));
    }

    /** 被凭证分录、收付款、固定资产、凭证科目设置（科目范围用途除外）使用（这些地方只能用末级科目） */
    private boolean usedInBooks(Long id) {
        return entryMapper.exists(Wrappers.<FinVoucherEntry>lambdaQuery().eq(FinVoucherEntry::getAccountId, id))
                || ruleMapper.exists(Wrappers.<FinAccountRule>lambdaQuery().eq(FinAccountRule::getAccountId, id)
                        .notIn(FinAccountRule::getRole, SCOPE_ROLES))
                || settlementMapper.exists(Wrappers.<FinSettlement>lambdaQuery().eq(FinSettlement::getAccountId, id))
                || assetMapper.exists(Wrappers.<FinAsset>lambdaQuery().eq(FinAsset::getExpenseAccountId, id));
    }

    /** 已使用：账簿中使用、编制了预算，或在凭证科目设置中被科目范围用途使用 */
    private boolean used(Long id) {
        return usedInBooks(id) || budgetMapper.exists(Wrappers.<FinBudget>lambdaQuery().eq(FinBudget::getAccountId, id))
                || ruleMapper.exists(Wrappers.<FinAccountRule>lambdaQuery().eq(FinAccountRule::getAccountId, id));
    }
}
