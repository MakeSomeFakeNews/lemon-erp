package com.lemon.erp.framework.rule;

import com.lemon.erp.framework.setting.OptionEnum;
import com.lemon.erp.framework.setting.SettingType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 业务参数定义：各业务模块通过 {@link BizRuleProvider} 登记，按租户保存（「系统管理 → 业务参数」修改），未修改的使用默认值。
 * 模块、名称、说明和选项、列的名称一律写消息键（由声明的模块登记），输出时 {@code Messages.get}
 * <p>
 * 表格型（TABLE）参数的值为文本：行之间用 {@code ;}、单元格之间用 {@code ,} 分隔，可为空的列留空表示「不限」，
 * 如 {@code 36000,3,0;,45,181920}，由 {@link #parseTable} 解析
 *
 * @param key          参数标识，如 scm.purchaseIn.overRate
 * @param module       所属模块（页面分组，消息键），如 供应链
 * @param label        名称（消息键）
 * @param type         类型：BOOL / NUMBER / DECIMAL / SELECT / TABLE（不支持 TEXT）
 * @param defaultValue 默认值（DECIMAL、TABLE 为去掉末尾 0 的规范写法，如 21.75、8）
 * @param options      SELECT 的取值枚举
 * @param min          NUMBER、DECIMAL 的最小值
 * @param max          NUMBER、DECIMAL 的最大值
 * @param scale        DECIMAL 的最多小数位数
 * @param columns      TABLE 的列
 * @param remark       说明（消息键）
 */
public record BizRule(String key, String module, String label, SettingType type, String defaultValue,
                      Class<? extends OptionEnum> options, int min, int max, int scale, List<Column> columns, String remark) {

    /** 行分隔符 */
    public static final String ROW_SEPARATOR = ";";
    /** 单元格分隔符 */
    public static final String CELL_SEPARATOR = ",";

    /**
     * 表格型参数的列
     *
     * @param label    列名（消息键）
     * @param scale    最多小数位数
     * @param min      最小值
     * @param max      最大值
     * @param nullable 可以留空（表示不限）
     */
    public record Column(String label, int scale, long min, long max, boolean nullable) {
    }

    public static BizRule bool(String key, String module, String label, boolean defaultValue, String remark) {
        return new BizRule(key, module, label, SettingType.BOOL, Boolean.toString(defaultValue), null, 0, 0, 0, null, remark);
    }

    public static BizRule number(String key, String module, String label, int defaultValue, int min, int max, String remark) {
        return new BizRule(key, module, label, SettingType.NUMBER, Integer.toString(defaultValue), null, min, max, 0, null, remark);
    }

    public static BizRule decimal(String key, String module, String label, String defaultValue, int scale, int min, int max,
                                  String remark) {
        return new BizRule(key, module, label, SettingType.DECIMAL, defaultValue, null, min, max, scale, null, remark);
    }

    public static BizRule select(String key, String module, String label, Enum<?> defaultValue, Class<? extends OptionEnum> options,
                                 String remark) {
        return new BizRule(key, module, label, SettingType.SELECT, defaultValue.name(), options, 0, 0, 0, null, remark);
    }

    public static BizRule table(String key, String module, String label, String defaultValue, List<Column> columns, String remark) {
        return new BizRule(key, module, label, SettingType.TABLE, defaultValue, null, 0, 0, 0, List.copyOf(columns), remark);
    }

    /**
     * 解析表格型参数的值：每行一个列表，留空的单元格为 null
     *
     * @throws NumberFormatException 单元格不是数字
     */
    public static List<List<BigDecimal>> parseTable(String value) {
        List<List<BigDecimal>> rows = new ArrayList<>();
        for (String line : value.split(ROW_SEPARATOR, -1)) {
            List<BigDecimal> row = new ArrayList<>();
            for (String cell : line.split(CELL_SEPARATOR, -1)) {
                String c = cell.trim();
                row.add(c.isEmpty() ? null : new BigDecimal(c));
            }
            rows.add(Collections.unmodifiableList(row));
        }
        return Collections.unmodifiableList(rows);
    }

    /** 表格型参数的规范写法（数字去掉末尾的 0） */
    public static String formatTable(List<List<BigDecimal>> rows) {
        List<String> lines = new ArrayList<>();
        for (List<BigDecimal> row : rows) {
            lines.add(String.join(CELL_SEPARATOR, row.stream().map(c -> c == null ? "" : plain(c)).toList()));
        }
        return String.join(ROW_SEPARATOR, lines);
    }

    /** 数字的规范写法：去掉末尾的 0，不用科学计数法 */
    public static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
