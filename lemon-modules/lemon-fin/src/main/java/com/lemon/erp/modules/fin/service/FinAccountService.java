package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import com.lemon.erp.modules.fin.model.AccountCategory;
import com.lemon.erp.modules.fin.model.AuxItem;
import com.lemon.erp.modules.fin.model.FinConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
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

    private static final Set<String> DIRECTIONS = Set.of("DEBIT", "CREDIT");

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
        return listAll().stream().filter(a -> a.getLeaf() && Integer.valueOf(1).equals(a.getStatus())).toList();
    }

    public FinAccount get(Long id) {
        FinAccount account = id == null ? null : accountMapper.selectById(id);
        if (account == null) {
            throw new BizException("科目不存在");
        }
        return account;
    }

    public void create(AccountRequest req) {
        FinAccount account = new FinAccount();
        fill(account, req, null);
        account.setStatus(1);
        try {
            accountMapper.insert(account);
        } catch (DuplicateKeyException e) {
            throw new BizException("科目编码已存在");
        }
    }

    /** 已使用的科目只能修改名称、备注、辅助核算 */
    public void update(Long id, AccountRequest req) {
        FinAccount old = get(id);
        if (used(id) || hasChildren(id)) {
            if (!old.getCode().equals(req.code()) || (req.direction() != null && !old.getDirection().equals(req.direction()))) {
                throw new BizException("科目已使用或有下级科目，只能修改名称、备注和辅助核算");
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
            throw new BizException("科目编码已存在");
        }
        // updateById 不更新 null 字段：取消全部辅助核算时单独清空
        if (account.getAuxItems() == null) {
            accountMapper.update(Wrappers.<FinAccount>lambdaUpdate().set(FinAccount::getAuxItems, null).eq(FinAccount::getId, id));
        }
    }

    public void changeStatus(Long id, Integer status) {
        get(id);
        accountMapper.update(Wrappers.<FinAccount>lambdaUpdate().set(FinAccount::getStatus, status).eq(FinAccount::getId, id));
    }

    public void delete(List<Long> ids) {
        for (FinAccount a : accountMapper.selectByIds(ids)) {
            if (hasChildren(a.getId())) {
                throw new BizException("科目「" + a.getCode() + " " + a.getName() + "」有下级科目，不能删除");
            }
            if (used(a.getId())) {
                throw new BizException("科目「" + a.getCode() + " " + a.getName() + "」已被使用，不能删除，可以停用");
            }
        }
        accountMapper.deleteByIds(ids);
    }

    // ---------------------------------------------------------------- 供其他服务使用

    /** 按编码取启用的末级科目，业务自动生成凭证使用 */
    public FinAccount leafByCode(String code) {
        FinAccount account = accountMapper.selectOne(Wrappers.<FinAccount>lambdaQuery().eq(FinAccount::getCode, code));
        if (account == null) {
            throw new BizException("缺少会计科目 " + code + "，请在「会计科目」中添加");
        }
        if (hasChildren(account.getId())) {
            throw new BizException("会计科目 " + code + " " + account.getName() + " 有下级科目，自动生成凭证需要末级科目");
        }
        if (!Integer.valueOf(1).equals(account.getStatus())) {
            throw new BizException("会计科目 " + code + " " + account.getName() + " 已停用");
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
                throw new BizException("科目不存在：" + id);
            }
            if (parents.contains(id)) {
                throw new BizException("科目「" + a.getCode() + " " + a.getName() + "」不是末级科目");
            }
            if (!Integer.valueOf(1).equals(a.getStatus())) {
                throw new BizException("科目「" + a.getCode() + " " + a.getName() + "」已停用");
            }
        }
        return map;
    }

    /** 结算科目：库存现金、银行存款及其下级的末级科目（收付款、发放工资） */
    public FinAccount requireCashAccount(Long accountId) {
        FinAccount account = requireLeaves(List.of(accountId)).get(accountId);
        if (!account.getCode().startsWith(FinConstants.CASH) && !account.getCode().startsWith(FinConstants.BANK)) {
            throw new BizException("结算账户必须是库存现金或银行存款科目");
        }
        return account;
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
                throw new BizException("一级科目编码为 4 位数字");
            }
            if (req.category() == null || !isCategory(req.category())) {
                throw new BizException("请选择科目类别");
            }
            if (req.direction() == null || !DIRECTIONS.contains(req.direction())) {
                throw new BizException("请选择余额方向");
            }
            account.setParentId(0L);
            account.setCategory(req.category());
            account.setDirection(req.direction());
            return;
        }
        FinAccount parent = get(req.parentId());
        if (parent.getId().equals(selfId)) {
            throw new BizException("上级科目不能是自己");
        }
        if (!code.startsWith(parent.getCode()) || code.length() != parent.getCode().length() + 2) {
            throw new BizException("下级科目编码为上级编码 " + parent.getCode() + " 后加 2 位数字");
        }
        if (selfId == null && usedInBooks(parent.getId())) {
            throw new BizException("上级科目「" + parent.getName() + "」已被使用，不能增加下级科目");
        }
        account.setParentId(parent.getId());
        account.setCategory(parent.getCategory());
        account.setDirection(req.direction() != null && DIRECTIONS.contains(req.direction()) ? req.direction() : parent.getDirection());
    }

    private static boolean isCategory(String category) {
        for (AccountCategory c : AccountCategory.values()) {
            if (c.name().equals(category)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasChildren(Long id) {
        return accountMapper.exists(Wrappers.<FinAccount>lambdaQuery().eq(FinAccount::getParentId, id));
    }

    /** 被凭证分录、收付款、固定资产、凭证科目设置使用（这些地方只能用末级科目） */
    private boolean usedInBooks(Long id) {
        return entryMapper.exists(Wrappers.<FinVoucherEntry>lambdaQuery().eq(FinVoucherEntry::getAccountId, id))
                || ruleMapper.exists(Wrappers.<FinAccountRule>lambdaQuery().eq(FinAccountRule::getAccountId, id))
                || settlementMapper.exists(Wrappers.<FinSettlement>lambdaQuery().eq(FinSettlement::getAccountId, id))
                || assetMapper.exists(Wrappers.<FinAsset>lambdaQuery().eq(FinAsset::getExpenseAccountId, id));
    }

    /** 已使用：账簿中使用，或编制了预算 */
    private boolean used(Long id) {
        return usedInBooks(id) || budgetMapper.exists(Wrappers.<FinBudget>lambdaQuery().eq(FinBudget::getAccountId, id));
    }
}
