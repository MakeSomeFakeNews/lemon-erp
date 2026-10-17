package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.cache.CacheService;
import com.lemon.erp.framework.rule.BizRule;
import com.lemon.erp.framework.rule.BizRuleProvider;
import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.framework.setting.OptionEnum;
import com.lemon.erp.framework.setting.SettingType;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.system.dto.BizRuleItem;
import com.lemon.erp.modules.system.dto.SettingItem;
import com.lemon.erp.modules.system.entity.SysBizRule;
import com.lemon.erp.modules.system.mapper.SysBizRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 业务参数：各模块登记的控制规则（如超收比例、信用控制方式），按租户保存修改过的值，读取时按租户缓存
 */
@Service
@RequiredArgsConstructor
public class SysBizRuleService implements BizRules {

    /** 缓存：按租户 */
    public static final String CACHE_KEY = "bizrule:";
    private static final Duration CACHE_TTL = Duration.ofHours(12);
    /** 参数值的最大长度（sys_biz_rule.rule_value） */
    private static final int MAX_VALUE_LENGTH = 255;

    private final SysBizRuleMapper ruleMapper;
    private final CacheService cache;
    private final List<BizRuleProvider> providers;

    private volatile Map<String, BizRule> definitions;
    /** 参数标识 → 登记它的模块（保存时的额外校验） */
    private volatile Map<String, BizRuleProvider> owners;

    @Override
    public String get(String key) {
        BizRule rule = definition(key);
        String value = saved().get(key);
        return value != null ? value : rule.defaultValue();
    }

    /** 全部参数及当前值（页面使用）：[{key, module, label, type, value, defaultValue, modified, options, min, max, scale, columns, remark}] */
    public List<BizRuleItem> list() {
        Map<String, String> saved = saved();
        return definitions().values().stream().map(r -> {
            String value = saved.getOrDefault(r.key(), r.defaultValue());
            // 模块、名称、选项、说明由各模块登记（消息键），按请求语言取消息
            List<SettingItem.Option> options = r.options() == null ? null : Arrays.stream(r.options().getEnumConstants())
                    .map(o -> new SettingItem.Option(o.name(), Messages.get(o.label()))).toList();
            List<BizRuleItem.Column> columns = r.columns() == null ? null : r.columns().stream()
                    .map(c -> new BizRuleItem.Column(Messages.get(c.label()), c.scale(), c.min(), c.max(), c.nullable())).toList();
            return new BizRuleItem(r.key(), Messages.get(r.module()), Messages.get(r.label()), r.type().name(), value, r.defaultValue(),
                    !Objects.equals(value, r.defaultValue()), options, r.min(), r.max(), r.scale(), columns, Messages.get(r.remark()));
        }).toList();
    }

    /** 保存（只包含要修改的参数），值为空或等于默认值时恢复默认 */
    @Transactional(rollbackFor = Exception.class)
    public void save(Map<String, String> values) {
        values.forEach((key, raw) -> {
            BizRule r = definition(key);
            String value = raw == null ? null : raw.trim();
            if (value != null && !value.isEmpty()) {
                value = validate(r, value);
                owners().get(key).validate(key, value);
            }
            boolean reset = value == null || value.isEmpty() || value.equals(r.defaultValue());
            SysBizRule row = ruleMapper.selectOne(Wrappers.<SysBizRule>lambdaQuery().eq(SysBizRule::getRuleKey, key));
            if (reset) {
                if (row != null) {
                    ruleMapper.deleteById(row.getId());
                }
            } else if (row == null) {
                SysBizRule created = new SysBizRule();
                created.setRuleKey(key);
                created.setRuleValue(value);
                ruleMapper.insert(created);
            } else {
                row.setRuleValue(value);
                ruleMapper.updateById(row);
            }
        });
        String key = cacheKey();
        evictNowAndAfterCommit(() -> cache.delete(key));
    }

    private BizRule definition(String key) {
        BizRule rule = key == null ? null : definitions().get(key);
        if (rule == null) {
            throw new BizException("system.error.businessRuleDoesNotExist", key);
        }
        return rule;
    }

    private Map<String, BizRule> definitions() {
        if (definitions == null) {
            Map<String, BizRule> map = new LinkedHashMap<>();
            Map<String, BizRuleProvider> owner = new HashMap<>();
            providers.forEach(p -> p.rules().forEach(r -> {
                map.put(r.key(), r);
                owner.put(r.key(), p);
            }));
            owners = owner;
            definitions = map;
        }
        return definitions;
    }

    private Map<String, BizRuleProvider> owners() {
        definitions();
        return owners;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> saved() {
        String key = cacheKey();
        Map<String, String> cached = cache.get(key, Map.class);
        if (cached != null) {
            return cached;
        }
        Map<String, String> map = new HashMap<>();
        ruleMapper.selectList(Wrappers.emptyWrapper()).forEach(r -> map.put(r.getRuleKey(), r.getRuleValue()));
        cache.set(key, map, CACHE_TTL);
        return map;
    }

    private static String cacheKey() {
        Long tenant = TenantContext.get();
        return CACHE_KEY + (tenant == null ? TenantContext.PLATFORM_TENANT_ID : tenant);
    }

    /** 校验取值，返回规范化之后的值（DECIMAL、TABLE 去掉数字末尾的 0） */
    private static String validate(BizRule r, String value) {
        String name = Messages.get(r.label());
        if (r.type() == SettingType.NUMBER) {
            int n;
            try {
                n = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new BizException("system.error.mustInteger", name);
            }
            if (n < r.min() || n > r.max()) {
                throw new BizException("system.error.mustBetween", name, r.min(), r.max());
            }
        } else if (r.type() == SettingType.BOOL) {
            if (!Boolean.TRUE.toString().equals(value) && !Boolean.FALSE.toString().equals(value)) {
                throw new BizException("system.error.mustTrueFalse", name);
            }
        } else if (r.type() == SettingType.SELECT) {
            if (Arrays.stream(r.options().getEnumConstants()).map(OptionEnum::name).noneMatch(value::equals)) {
                throw new BizException("system.error.invalidValue", name, value);
            }
        } else if (r.type() == SettingType.DECIMAL) {
            BigDecimal n = decimal(value, name);
            if (n.compareTo(BigDecimal.valueOf(r.min())) < 0 || n.compareTo(BigDecimal.valueOf(r.max())) > 0) {
                throw new BizException("system.error.mustBetween", name, r.min(), r.max());
            }
            if (n.stripTrailingZeros().scale() > r.scale()) {
                throw new BizException("system.error.decimalPlaces", name, r.scale());
            }
            return BizRule.plain(n);
        } else if (r.type() == SettingType.TABLE) {
            return validateTable(r, value, name);
        } else {
            throw new BizException("system.error.typeNotSupported", name);
        }
        return value;
    }

    private static String validateTable(BizRule r, String value, String name) {
        List<List<BigDecimal>> rows;
        try {
            rows = BizRule.parseTable(value);
        } catch (NumberFormatException e) {
            throw new BizException("system.error.mustNumber", name);
        }
        for (int i = 0; i < rows.size(); i++) {
            List<BigDecimal> row = rows.get(i);
            if (row.size() != r.columns().size()) {
                throw new BizException("system.error.tableColumns", name, i + 1, r.columns().size());
            }
            for (int j = 0; j < row.size(); j++) {
                BizRule.Column c = r.columns().get(j);
                BigDecimal n = row.get(j);
                String column = Messages.get(c.label());
                if (n == null) {
                    if (!c.nullable()) {
                        throw new BizException("system.error.tableCellRequired", name, i + 1, column);
                    }
                    continue;
                }
                if (n.compareTo(BigDecimal.valueOf(c.min())) < 0 || n.compareTo(BigDecimal.valueOf(c.max())) > 0) {
                    throw new BizException("system.error.tableCellBetween", name, i + 1, column, c.min(), c.max());
                }
                if (n.stripTrailingZeros().scale() > c.scale()) {
                    throw new BizException("system.error.tableCellDecimalPlaces", name, i + 1, column, c.scale());
                }
            }
        }
        String normalized = BizRule.formatTable(rows);
        if (normalized.length() > MAX_VALUE_LENGTH) {
            throw new BizException("system.error.mustCharactersLong2", name, 1, MAX_VALUE_LENGTH);
        }
        return normalized;
    }

    private static BigDecimal decimal(String value, String name) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new BizException("system.error.mustNumber", name);
        }
    }

    /** 立即清理一次，有事务时提交后再清理一次，避免并发请求把未提交前的旧值重新加载进缓存 */
    private static void evictNowAndAfterCommit(Runnable evict) {
        evict.run();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict.run();
                }
            });
        }
    }
}
