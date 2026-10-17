package com.lemon.erp.modules.system.dto;

import java.util.List;

/**
 * 业务参数页面的一项参数（模块、名称、选项、说明已按请求语言翻译）
 *
 * @param type         BOOL / NUMBER / DECIMAL / SELECT / TABLE
 * @param value        当前值
 * @param defaultValue 默认值
 * @param modified     是否修改过（不是默认值）
 * @param options      SELECT 的选项
 * @param min          NUMBER、DECIMAL 的取值范围
 * @param scale        DECIMAL 的小数位数
 * @param columns      TABLE 的列
 */
public record BizRuleItem(String key, String module, String label, String type, String value, String defaultValue,
                          boolean modified, List<SettingItem.Option> options, int min, int max, int scale, List<Column> columns,
                          String remark) {

    /**
     * 表格型参数的列
     *
     * @param nullable 可以留空（表示不限）
     */
    public record Column(String label, int scale, long min, long max, boolean nullable) {
    }
}
