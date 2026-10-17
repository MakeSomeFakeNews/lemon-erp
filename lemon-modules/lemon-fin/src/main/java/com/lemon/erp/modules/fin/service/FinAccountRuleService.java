package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.entity.FinAccountRule;
import com.lemon.erp.modules.fin.mapper.FinAccountRuleMapper;
import com.lemon.erp.modules.fin.model.AccountRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 凭证科目设置：业务单据自动生成凭证时，按用途（{@link AccountRole}）取科目。
 * 优先级：用途 + 维度取值的设置 → 用途的默认设置 → 后备用途（{@link AccountRole#fallback()}）的设置 → 预置科目编码（按维度取值区分，如半成品、成品为库存商品）
 */
@Service
@RequiredArgsConstructor
public class FinAccountRuleService {

    private static final String NO_DIM = "";

    private final FinAccountRuleMapper ruleMapper;
    private final FinAccountService accountService;

    /** 用途使用的末级科目 ID */
    public Long accountId(AccountRole role) {
        return account(role, null).getId();
    }

    /**
     * 用途在某个维度取值下使用的末级科目 ID
     *
     * @param dimValue 物料分类 / 职位类别，可为空
     */
    public Long accountId(AccountRole role, String dimValue) {
        return account(role, dimValue).getId();
    }

    /** 用途在某个维度取值下使用的科目编码（存货对账等按编码比较的场景） */
    public String code(AccountRole role, String dimValue) {
        return account(role, dimValue).getCode();
    }

    public FinAccount account(AccountRole role, String dimValue) {
        if (role.scope()) {
            // 科目范围用途不用于生成凭证，按编码前缀使用 FinAccountService.roleCode
            throw new IllegalArgumentException("scope role " + role);
        }
        String dim = dimValue == null || role.dimension() == AccountRole.Dimension.NONE ? NO_DIM : dimValue;
        List<FinAccountRule> rules = ruleMapper.selectList(Wrappers.<FinAccountRule>lambdaQuery()
                .eq(FinAccountRule::getRole, role)
                .in(FinAccountRule::getDimValue, dim.isEmpty() ? List.of(NO_DIM) : List.of(dim, NO_DIM)));
        FinAccountRule rule = rules.stream().filter(r -> dim.equals(r.getDimValue())).findFirst()
                .orElseGet(() -> rules.stream().filter(r -> NO_DIM.equals(r.getDimValue())).findFirst().orElse(null));
        if (rule == null && role.fallback() != null) {
            return account(role.fallback(), null);
        }
        if (rule == null) {
            return accountService.leafByCode(role.defaultCode(dim.isEmpty() ? null : dim));
        }
        try {
            return accountService.requireLeaves(List.of(rule.getAccountId())).get(rule.getAccountId());
        } catch (BizException e) {
            throw new BizException("fin.error.voucherAccountSetting", Messages.get(role.labelKey()), e.getLocalizedMessage());
        }
    }

    /** 全部用途及其设置，供「凭证科目设置」页面展示 */
    public List<RoleView> list() {
        Map<AccountRole, List<FinAccountRule>> byRole = ruleMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(FinAccountRule::getRole));
        List<RoleView> views = new ArrayList<>();
        for (AccountRole role : AccountRole.values()) {
            List<FinAccountRule> rules = byRole.getOrDefault(role, List.of());
            Long accountId = rules.stream().filter(r -> NO_DIM.equals(r.getDimValue())).map(FinAccountRule::getAccountId)
                    .findFirst().orElse(null);
            Map<String, Long> items = new LinkedHashMap<>();
            rules.stream().filter(r -> !NO_DIM.equals(r.getDimValue())).forEach(r -> items.put(r.getDimValue(), r.getAccountId()));
            // 分组、用途名称返回给页面显示，按请求语言翻译
            views.add(new RoleView(role.name(), Messages.get(role.groupKey()), Messages.get(role.labelKey()), role.dimension().name(), role.dimension().dictType(),
                    role.defaultCode(), role.dimensionDefaults(), role.fallback() == null ? null : role.fallback().name(), role.scope(), accountId, items));
        }
        return views;
    }

    /**
     * 保存一个用途的设置（整体替换）：未填写的使用预置科目
     *
     * @param accountId 用途的默认科目，可为空
     * @param items     维度取值 → 科目，只有带维度的用途可以填写
     */
    @Transactional
    public void save(AccountRole role, Long accountId, Map<String, Long> items) {
        Map<String, Long> byDim = new LinkedHashMap<>();
        if (items != null) {
            items.forEach((dim, id) -> {
                if (id != null) {
                    byDim.put(dim, id);
                }
            });
        }
        if (!byDim.isEmpty() && role.dimension() == AccountRole.Dimension.NONE) {
            throw new BizException("fin.error.cannotSetDimension", Messages.get(role.labelKey()));
        }
        if (byDim.keySet().stream().anyMatch(d -> d == null || d.isBlank() || d.length() > 32)) {
            throw new BizException("fin.error.invalidDimensionValue");
        }
        Set<Long> ids = new HashSet<>(byDim.values());
        if (accountId != null) {
            ids.add(accountId);
        }
        if (role.scope() && accountId != null) {
            // 科目范围用途可以设置上级科目，只要求启用
            FinAccount account = accountService.get(accountId);
            if (!Integer.valueOf(1).equals(account.getStatus())) {
                throw new BizException("fin.error.accountDisabled2", account.getCode(), account.getName());
            }
        } else if (!ids.isEmpty()) {
            accountService.requireLeaves(ids);
        }
        ruleMapper.delete(Wrappers.<FinAccountRule>lambdaQuery().eq(FinAccountRule::getRole, role));
        if (accountId != null) {
            insert(role, NO_DIM, accountId);
        }
        byDim.forEach((dim, id) -> insert(role, dim, id));
    }

    private void insert(AccountRole role, String dim, Long accountId) {
        FinAccountRule rule = new FinAccountRule();
        rule.setRole(role);
        rule.setDimValue(dim);
        rule.setAccountId(accountId);
        ruleMapper.insert(rule);
    }

    /**
     * 一个用途的设置
     *
     * @param dimension         维度（NONE、MATERIAL_CATEGORY、POSITION_CATEGORY、IN_CATEGORY、OUT_CATEGORY）
     * @param dictType          维度取值所在的字典
     * @param defaultCode       预置科目编码
     * @param dimensionDefaults 维度取值 → 预置科目编码（没有的取 defaultCode）
     * @param fallback          没有设置时沿用的用途，没有为空
     * @param scope             科目范围用途：设置的科目及其下级都属于该用途，可以选上级科目
     * @param accountId         设置的默认科目，未设置为空
     * @param items             维度取值 → 设置的科目
     */
    public record RoleView(String role, String group, String label, String dimension, String dictType, String defaultCode,
                           Map<String, String> dimensionDefaults, String fallback, boolean scope, Long accountId, Map<String, Long> items) {
    }
}
