package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.framework.cache.CacheService;
import com.lemon.erp.framework.setting.OptionEnum;
import com.lemon.erp.framework.setting.PasswordComplexity;
import com.lemon.erp.framework.setting.Setting;
import com.lemon.erp.framework.setting.SettingService;
import com.lemon.erp.framework.setting.SettingType;
import com.lemon.erp.modules.system.dto.SettingItem;
import com.lemon.erp.modules.system.entity.SysConfig;
import com.lemon.erp.modules.system.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 系统参数（参数设置）：已修改的参数保存在 sys_config 并整体缓存，未修改的取默认值
 */
@Service
@RequiredArgsConstructor
public class SysConfigService implements SettingService {

    public static final String CACHE_KEY = "settings:all";
    private static final Duration CACHE_TTL = Duration.ofHours(12);

    private final SysConfigMapper configMapper;
    private final CacheService cache;
    private final Environment env;

    @Override
    public String get(Setting setting) {
        String value = saved().get(setting.key());
        return value != null ? value : defaultValue(setting);
    }

    /** 默认值：配置文件中的属性优先 */
    public String defaultValue(Setting setting) {
        return setting.property() == null ? setting.defaultValue() : env.getProperty(setting.property(), setting.defaultValue());
    }

    public List<SettingItem> list() {
        Map<String, String> saved = saved();
        return Arrays.stream(Setting.values()).map(s -> {
            String def = defaultValue(s);
            String value = saved.getOrDefault(s.key(), def);
            List<SettingItem.Option> options = s.options() == null ? null : Arrays.stream(s.options().getEnumConstants())
                    .map(o -> new SettingItem.Option(o.name(), I18n.t(o.label()))).toList();
            // 分组、名称、选项、说明按请求语言翻译
            return new SettingItem(s.key(), s.group().name(), I18n.t(s.group().label()), I18n.t(s.label()), s.type().name(), value, def,
                    !Objects.equals(value, def), options, s.min(), s.max(), I18n.t(s.remark()));
        }).toList();
    }

    /**
     * 保存参数：值为空或等于默认值时恢复默认（删除保存的值）
     *
     * @param values 参数标识 → 值，只包含要修改的参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(Map<String, String> values) {
        Map<Setting, String> changes = new HashMap<>();
        values.forEach((key, raw) -> {
            Setting s = Setting.ofKey(key);
            if (s == null) {
                throw new BizException("参数不存在：{0}", key);
            }
            String value = raw == null ? null : raw.trim();
            if (value != null && !value.isEmpty()) {
                validate(s, value);
            }
            changes.put(s, value == null || value.isEmpty() || value.equals(defaultValue(s)) ? null : value);
        });
        // 初始密码须符合（修改后的）密码策略
        String initial = valueAfter(changes, Setting.USER_INITIAL_PASSWORD);
        PasswordPolicy.check(initial, Integer.parseInt(valueAfter(changes, Setting.PASSWORD_MIN_LENGTH)),
                PasswordComplexity.valueOf(valueAfter(changes, Setting.PASSWORD_COMPLEXITY)), "初始密码");

        changes.forEach((s, value) -> {
            SysConfig row = configMapper.selectOne(Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getConfigKey, s.key()));
            if (value == null) {
                if (row != null) {
                    configMapper.deleteById(row.getId());
                }
            } else if (row == null) {
                SysConfig created = new SysConfig();
                created.setConfigKey(s.key());
                created.setConfigValue(value);
                configMapper.insert(created);
            } else if (!value.equals(row.getConfigValue())) {
                row.setConfigValue(value);
                configMapper.updateById(row);
            }
        });
        evict();
    }

    private String valueAfter(Map<Setting, String> changes, Setting s) {
        if (!changes.containsKey(s)) {
            return get(s);
        }
        String value = changes.get(s);
        return value == null ? defaultValue(s) : value;
    }

    private static void validate(Setting s, String value) {
        String name = I18n.t(s.label());
        if (s.type() == SettingType.NUMBER) {
            int n;
            try {
                n = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new BizException("「{0}」必须是整数", name);
            }
            if (n < s.min() || n > s.max()) {
                throw new BizException("「{0}」的范围是 {1} - {2}", name, s.min(), s.max());
            }
        } else if (s.type() == SettingType.BOOL) {
            if (!Boolean.TRUE.toString().equals(value) && !Boolean.FALSE.toString().equals(value)) {
                throw new BizException("「{0}」只能是 true 或 false", name);
            }
        } else if (s.type() == SettingType.SELECT) {
            if (Arrays.stream(s.options().getEnumConstants()).map(OptionEnum::name).noneMatch(value::equals)) {
                throw new BizException("「{0}」的取值无效：{1}", name, value);
            }
        } else if (value.length() < s.min() || value.length() > s.max()) {
            throw new BizException("「{0}」的长度为 {1} - {2} 个字符", name, s.min(), s.max());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> saved() {
        Map<String, String> cached = cache.get(CACHE_KEY, Map.class);
        if (cached != null) {
            return cached;
        }
        Map<String, String> loaded = new HashMap<>();
        configMapper.selectList(null).forEach(c -> loaded.put(c.getConfigKey(), c.getConfigValue()));
        cache.set(CACHE_KEY, loaded, CACHE_TTL);
        return loaded;
    }

    /** 提交后清理缓存，避免并发请求把旧值重新加载进缓存 */
    private void evict() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cache.delete(CACHE_KEY);
                }
            });
        }
        cache.delete(CACHE_KEY);
    }
}
