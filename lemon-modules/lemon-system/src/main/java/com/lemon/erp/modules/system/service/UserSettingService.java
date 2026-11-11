package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.system.entity.SysUserSetting;
import com.lemon.erp.modules.system.mapper.SysUserSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 用户的个人设置（界面设置等）：按设置项保存 JSON 对象，只读写当前登录用户自己的。
 * 只在登录后读取一次（随 /auth/info 返回），按 (user_id, setting_key, tenant_id) 唯一索引查询，不另加缓存
 */
@Service
@RequiredArgsConstructor
public class UserSettingService {

    /** 界面设置：主题、主题色、布局、多标签页、紧凑模式等，由前端解析 */
    public static final String UI = "ui";

    private static final Pattern KEY_PATTERN = Pattern.compile("[a-z][a-zA-Z0-9_]{0,31}");
    /** 单项设置序列化后的最大长度（字符） */
    static final int MAX_LENGTH = 8000;
    /** 每个用户最多的设置项数 */
    static final int MAX_KEYS = 20;

    private final SysUserSettingMapper settingMapper;
    private final ObjectMapper objectMapper;

    /** 当前用户的某项设置，没有保存过时为 null */
    public Map<String, Object> get(String key) {
        checkKey(key);
        return find(SecurityUtils.getUserId(), key);
    }

    /** 保存当前用户的某项设置（整体替换） */
    public void save(String key, Map<String, Object> value) {
        checkKey(key);
        if (value == null) {
            throw new BizException("system.error.userSettingRequired");
        }
        String json = objectMapper.writeValueAsString(value);
        if (json.length() > MAX_LENGTH) {
            throw new BizException("system.error.userSettingTooLarge", String.valueOf(MAX_LENGTH));
        }
        Long userId = SecurityUtils.getUserId();
        if (updateValue(userId, key, json) > 0) {
            return;
        }
        if (settingMapper.selectCount(Wrappers.<SysUserSetting>lambdaQuery().eq(SysUserSetting::getUserId, userId)) >= MAX_KEYS) {
            throw new BizException("system.error.userSettingTooMany", String.valueOf(MAX_KEYS));
        }
        SysUserSetting setting = new SysUserSetting();
        setting.setUserId(userId);
        setting.setSettingKey(key);
        setting.setSettingValue(json);
        try {
            settingMapper.insert(setting);
        } catch (DuplicateKeyException e) {
            // 同一用户的两次保存同时到达：另一次已插入，改为更新
            updateValue(userId, key, json);
        }
    }

    /** 删除用户时一并删除其设置 */
    void deleteByUsers(Collection<Long> userIds) {
        if (!userIds.isEmpty()) {
            settingMapper.delete(Wrappers.<SysUserSetting>lambdaQuery().in(SysUserSetting::getUserId, userIds));
        }
    }

    Map<String, Object> find(Long userId, String key) {
        SysUserSetting setting = settingMapper.selectOne(Wrappers.<SysUserSetting>lambdaQuery()
                .eq(SysUserSetting::getUserId, userId).eq(SysUserSetting::getSettingKey, key));
        if (setting == null || setting.getSettingValue() == null) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> value = objectMapper.readValue(setting.getSettingValue(), LinkedHashMap.class);
            return value;
        } catch (RuntimeException e) {
            // 内容损坏（如直接改库）时当作没有保存过，前端使用默认值
            return null;
        }
    }

    private int updateValue(Long userId, String key, String json) {
        SysUserSetting update = new SysUserSetting();
        update.setSettingValue(json);
        return settingMapper.update(update, Wrappers.<SysUserSetting>lambdaUpdate()
                .eq(SysUserSetting::getUserId, userId).eq(SysUserSetting::getSettingKey, key));
    }

    private static void checkKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new BizException("system.error.userSettingKeyInvalid");
        }
    }
}
