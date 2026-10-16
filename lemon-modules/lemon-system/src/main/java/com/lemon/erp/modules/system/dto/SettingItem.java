package com.lemon.erp.modules.system.dto;

import java.util.List;

/**
 * 参数设置页面的一项参数
 *
 * @param value        当前值
 * @param defaultValue 默认值（配置文件或内置）
 * @param modified     是否修改过（不是默认值）
 * @param options      SELECT 的选项
 * @param min          NUMBER 的取值范围、TEXT 的长度范围
 */
public record SettingItem(String key, String group, String groupLabel, String label, String type, String value,
                          String defaultValue, boolean modified, List<Option> options, int min, int max, String remark) {

    public record Option(String value, String label) {
    }
}
