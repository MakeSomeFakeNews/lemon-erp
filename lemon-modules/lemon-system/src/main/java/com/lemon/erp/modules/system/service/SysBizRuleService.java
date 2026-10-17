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
import com.lemon.erp.modules.system.entity.SysBizRule;
import com.lemon.erp.modules.system.mapper.SysBizRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final SysBizRuleMapper ruleMapper;
    private final CacheService cache;
    private final List<BizRuleProvider> providers;

    private volatile Map<String, BizRule> definitions;

    @Override
    public String get(String key) {
        BizRule rule = definition(key);
        String value = saved().get(key);
        return value != null ? value : rule.defaultValue();
    }

    /** 全部参数及当前值（页面使用）：[{key, module, label, type, value, defaultValue, modified, options, min, max, remark}] */
    public List<Map<String, Object>> list() {
        Map<String, String> saved = saved();
        return definitions().values().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            String value = saved.getOrDefault(r.key(), r.defaultValue());
            m.put("key", r.key());
            // 模块、名称、选项、说明由各模块登记（消息键），按请求语言取消息
            m.put("module", Messages.get(r.module()));
            m.put("label", Messages.get(r.label()));
            m.put("type", r.type().name());
            m.put("value", value);
            m.put("defaultValue", r.defaultValue());
            m.put("modified", !Objects.equals(value, r.defaultValue()));
            m.put("options", r.options() == null ? null : Arrays.stream(r.options().getEnumConstants())
                    .map(o -> Map.of("value", o.name(), "label", Messages.get(o.label()))).toList());
            m.put("min", r.min());
            m.put("max", r.max());
            m.put("remark", Messages.get(r.remark()));
            return m;
        }).toList();
    }

    /** 保存（只包含要修改的参数），值为空或等于默认值时恢复默认 */
    @Transactional(rollbackFor = Exception.class)
    public void save(Map<String, String> values) {
        values.forEach((key, raw) -> {
            BizRule r = definition(key);
            String value = raw == null ? null : raw.trim();
            if (value != null && !value.isEmpty()) {
                validate(r, value);
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
        cache.delete(cacheKey());
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
            providers.forEach(p -> p.rules().forEach(r -> map.put(r.key(), r)));
            definitions = map;
        }
        return definitions;
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

    private static void validate(BizRule r, String value) {
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
        } else {
            throw new BizException("system.error.typeNotSupported", name);
        }
    }
}
