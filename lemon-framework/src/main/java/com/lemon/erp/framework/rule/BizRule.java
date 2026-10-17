package com.lemon.erp.framework.rule;

import com.lemon.erp.framework.setting.OptionEnum;
import com.lemon.erp.framework.setting.SettingType;

/**
 * 业务参数定义：各业务模块通过 {@link BizRuleProvider} 登记，按租户保存（「系统管理 → 业务参数」修改），未修改的使用默认值
 *
 * @param key          参数标识，如 scm.purchaseIn.overRate
 * @param module       所属模块（页面分组），如 供应链
 * @param label        名称
 * @param type         类型：BOOL / NUMBER / SELECT（不支持 TEXT）
 * @param defaultValue 默认值
 * @param options      SELECT 的取值枚举
 * @param min          NUMBER 的最小值
 * @param max          NUMBER 的最大值
 * @param remark       说明
 */
public record BizRule(String key, String module, String label, SettingType type, String defaultValue,
                      Class<? extends OptionEnum> options, int min, int max, String remark) {

    public static BizRule bool(String key, String module, String label, boolean defaultValue, String remark) {
        return new BizRule(key, module, label, SettingType.BOOL, Boolean.toString(defaultValue), null, 0, 0, remark);
    }

    public static BizRule number(String key, String module, String label, int defaultValue, int min, int max, String remark) {
        return new BizRule(key, module, label, SettingType.NUMBER, Integer.toString(defaultValue), null, min, max, remark);
    }

    public static BizRule select(String key, String module, String label, Enum<?> defaultValue, Class<? extends OptionEnum> options,
                                 String remark) {
        return new BizRule(key, module, label, SettingType.SELECT, defaultValue.name(), options, 0, 0, remark);
    }
}
